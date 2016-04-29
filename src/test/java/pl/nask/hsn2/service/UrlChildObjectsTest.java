/*
 * Copyright (c) NASK, NCSC
 *
 * This file is part of HoneySpider Network 2.1.
 *
 * This is a free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.nask.hsn2.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import mockit.Delegate;
import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import pl.nask.hsn2.ResourceException;
import pl.nask.hsn2.ServiceConnector;
import pl.nask.hsn2.StorageException;
import pl.nask.hsn2.bus.operations.ObjectData;
import pl.nask.hsn2.bus.operations.ObjectResponse;
import pl.nask.hsn2.bus.operations.builder.ObjectResponseBuilder;
import pl.nask.hsn2.connector.REST.DataResponse;
import pl.nask.hsn2.server.TestHttpServer;
import pl.nask.hsn2.service.task.WebClientTaskContext;
import pl.nask.hsn2.service.urlfollower.HtmlUnitFollower;
import pl.nask.hsn2.service.urlfollower.OutgoingLink;
import pl.nask.hsn2.service.urlfollower.PageLinks;
import pl.nask.hsn2.utils.MockTestsHelper;
import pl.nask.hsn2.utils.WebClientDataStoreHelper;

public class UrlChildObjectsTest {
	public static final String REFERRER = "http://referrer/referrer.html";
	@Mocked
	ServiceConnector connector;
	@Mocked
	WebClientDataStoreHelper wcHelper;
	WebClientTaskContext jobContext;
	ServiceParameters params;
	HtmlUnitFollower follower;
	WebClientTask webClientTask;
	private int newObjectsInOSCounter;
	private Set<String> newObjectStoreObjects;

	@BeforeClass
	public void startServer() throws Exception {
		TestHttpServer.startServer("urlChilds");
	}

	@AfterClass
	public void stopServer() throws Exception {
		TestHttpServer.stopServer();
	}

	private void initTestData(String absUrl) throws Exception {
		expectationsSetup();
		params = new ServiceParameters();
		params.setSaveHtml(false);
		jobContext = new WebClientTaskContext(1, 1, 1, connector);
		ServiceData serviceData = new ServiceData(1l, absUrl, absUrl, REFERRER, 1l, 5, 1l, null);
		follower = new HtmlUnitFollower(absUrl, jobContext, params);
		webClientTask = new WebClientTask(jobContext, params, serviceData, follower);
	}

	@SuppressWarnings({ "unchecked", "unused", "rawtypes" })
	private void expectationsSetup() throws Exception {
		newObjectsInOSCounter = 0;
		newObjectStoreObjects = new TreeSet<>();

		final DataResponse dataResponse = new DataResponse(1L);
		final ObjectResponse saveObjectsResponse = new ObjectResponse(pl.nask.hsn2.bus.operations.ObjectResponse.ResponseType.SUCCESS_PUT);
		saveObjectsResponse.setObjects(Collections.singleton(1L));
		new NonStrictExpectations() {
			{
				// Sends data to Data Store as a byte array.
				connector.sendDataStoreData(anyLong, withInstanceOf(byte[].class));
				result = dataResponse;

				// Sends data to Data Store as an input stream.
				connector.sendDataStoreData(anyLong, withInstanceOf(InputStream.class));
				result = dataResponse;

				// Saving new objects to Object Store.
				connector.saveObjects(anyLong, withInstanceOf(List.class));
				result = new Delegate() {
					public ObjectResponse saveObjects(long jobId, List<ObjectData> objects) {
						ObjectResponseBuilder builder = new ObjectResponseBuilder(pl.nask.hsn2.bus.operations.ObjectResponse.ResponseType.SUCCESS_PUT);
						for (int i = 1; i <= objects.size(); i++) {
							builder.addObject(i);
						}
						ObjectResponse saveObjectsResponse = builder.build();
						return saveObjectsResponse;
					}
				};
				forEachInvocation = new Object() {
					void validate(long jobId, List<ObjectData> dataList) {
						newObjectsInOSCounter = MockTestsHelper.saveNewObjectInObjectStore(dataList, newObjectStoreObjects, newObjectsInOSCounter);
					}
				};
			}
		};
	}

	private void assertIsOnOutgoingAbsoluteList(Set<OutgoingLink> set, String relativeUrl) {
		Iterator<OutgoingLink> it = set.iterator();
		while (it.hasNext()) {
			OutgoingLink e = it.next();
			if (e.getAbsoluteUrl().equals(relativeUrl)) {
				return;
			}
		}
		Assert.fail("Resource not found: " + relativeUrl);
	}

	@Test
	public void aHrefLink() throws Exception {
		initTestData(TestHttpServer.absoluteUrl("aHrefLink.html"));
		params.setSaveObjects(true);
		follower.processUrl();
		PageLinks links = follower.getPageLinks();
		Assert.assertEquals(links.getOutgoingLinks().size(), 2);
		assertIsOnOutgoingAbsoluteList(links.getOutgoingLinks(), "http://www.apache.org/");
		assertIsOnOutgoingAbsoluteList(links.getOutgoingLinks(), "http://www.google.pl");
	}

	@Test
	public void areaHrefLink() throws Exception {
		initTestData(TestHttpServer.absoluteUrl("areaHrefLink.html"));
		params.setSaveObjects(true);
		follower.processUrl();
		PageLinks links = follower.getPageLinks();
		Assert.assertEquals(links.getOutgoingLinks().size(), 3);
		assertIsOnOutgoingAbsoluteList(links.getOutgoingLinks(), "http://www.gentoo.org/");
		assertIsOnOutgoingAbsoluteList(links.getOutgoingLinks(), "https://www.suse.com/");
		assertIsOnOutgoingAbsoluteList(links.getOutgoingLinks(), "http://www.debian.org");
	}

	@Test
	public void resourcesInFramesTest() throws Exception {
		// Init.
		initTestData(TestHttpServer.absoluteUrl("frames.html"));
		params.setSaveImages(true);

		// Go.
		webClientTask.process();
		jobContext.flush();

		// Asserts.
		Assert.assertTrue(follower.isSuccessfull());
		Assert.assertEquals(newObjectsInOSCounter, 12);

		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("frameTestHelper1.html") + ";url;frame"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("frameTestHelper2.html") + ";url;frame"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("aaa.png") + ";file"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("aaa.png") + ";url;embedded"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("bbb.png") + ";file"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("bbb.png") + ";url;embedded"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("ccc.png") + ";file"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("ccc.png") + ";url;embedded"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("ddd.png") + ";file"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("ddd.png") + ";url;embedded"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("eee.png") + ";file"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("eee.png") + ";url;embedded"));
	}

	@Test
	public void resourcesInIframesTest() throws Exception {
		// Init.
		initTestData(TestHttpServer.absoluteUrl("iframes.html"));
		params.setSaveImages(true);

		// Go.
		webClientTask.process();
		jobContext.flush();

		// Asserts.
		Assert.assertTrue(follower.isSuccessfull());
		Assert.assertEquals(newObjectsInOSCounter, 12);

		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("frameTestHelper1.html") + ";url;iframe"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("frameTestHelper2.html") + ";url;iframe"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("aaa.png") + ";file"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("aaa.png") + ";url;embedded"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("bbb.png") + ";file"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("bbb.png") + ";url;embedded"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("ccc.png") + ";file"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("ccc.png") + ";url;embedded"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("ddd.png") + ";file"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("ddd.png") + ";url;embedded"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("eee.png") + ";file"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("eee.png") + ";url;embedded"));
	}

	@Test
	public void imgLongdescLongdesc() throws Exception {
		initTestData(TestHttpServer.absoluteUrl("imgLongdesc.html"));
		params.setProcessExternalLinks(true);
		follower.processUrl();
		PageLinks links = follower.getPageLinks();
		Assert.assertEquals(links.getOutgoingLinks().size(), 1);
		assertIsOnOutgoingAbsoluteList(links.getOutgoingLinks(), TestHttpServer.absoluteUrl("imgLongdesc_desc.txt"));
	}

	@Test
	public void htmlBaseTagChildLinks() throws Exception {
		// test for outgoing links when BASE tag is present
		initTestData(TestHttpServer.absoluteUrl("htmlBaseTagTest.html"));
		follower.processUrl();
		PageLinks links = follower.getPageLinks();
		Assert.assertEquals(links.getOutgoingLinks().size(), 6);
		assertIsOnOutgoingAbsoluteList(links.getOutgoingLinks(), "http://some.page/longdesc.txt");
		assertIsOnOutgoingAbsoluteList(links.getOutgoingLinks(), "http://www.nask.pl/desc.txt");
		assertIsOnOutgoingAbsoluteList(links.getOutgoingLinks(), "http://some.page/link.html");
		assertIsOnOutgoingAbsoluteList(links.getOutgoingLinks(), "http://aaa.bbb.cc");
		assertIsOnOutgoingAbsoluteList(links.getOutgoingLinks(), "http://some.page/rect.html");
		assertIsOnOutgoingAbsoluteList(links.getOutgoingLinks(), "http://www.google.pl/");
	}

	@Test
	public void htmlBaseFramesTest() throws Exception, StorageException, ResourceException, IOException {
		// Init.
		initTestData(TestHttpServer.absoluteUrl("htmlBaseTagFrames.html"));
		params.setSaveImages(true);

		// Go.
		webClientTask.process();
		jobContext.flush();

		// Asserts.
		Assert.assertTrue(follower.isSuccessfull());
		Assert.assertEquals(newObjectsInOSCounter, 12);

		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("baseTest/frameBaseTestHelper1.html") + ";url;frame"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("baseTest/frameBaseTestHelper2.html") + ";url;frame"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("baseTest/aaaBase.png") + ";url;embedded"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("baseTest/aaaBase.png") + ";file"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("baseTest/bbbBase.png") + ";url;embedded"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("baseTest/bbbBase.png") + ";file"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("baseTest/cccBase.png") + ";url;embedded"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("baseTest/cccBase.png") + ";file"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("baseTest/dddBase.png") + ";url;embedded"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("baseTest/dddBase.png") + ";file"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("baseTest/eeeBase.png") + ";url;embedded"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("baseTest/eeeBase.png") + ";file"));
	}

	@Test
	public void htmlBaseIframesTest() throws Exception {
		initTestData(TestHttpServer.absoluteUrl("htmlBaseTagIframes.html"));
		params.setSaveImages(true);

		// Go.
		webClientTask.process();
		jobContext.flush();

		// Asserts.
		Assert.assertTrue(follower.isSuccessfull());
		Assert.assertEquals(newObjectsInOSCounter, 12);

		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("baseTest/frameBaseTestHelper1.html") + ";url;iframe"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("baseTest/frameBaseTestHelper2.html") + ";url;iframe"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("baseTest/aaaBase.png") + ";url;embedded"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("baseTest/aaaBase.png") + ";file"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("baseTest/bbbBase.png") + ";url;embedded"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("baseTest/bbbBase.png") + ";file"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("baseTest/cccBase.png") + ";url;embedded"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("baseTest/cccBase.png") + ";file"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("baseTest/dddBase.png") + ";url;embedded"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("baseTest/dddBase.png") + ";file"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("baseTest/eeeBase.png") + ";url;embedded"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("baseTest/eeeBase.png") + ";file"));
	}
}
