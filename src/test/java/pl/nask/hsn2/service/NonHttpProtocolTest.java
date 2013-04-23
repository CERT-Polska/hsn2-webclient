/*
 * Copyright (c) NASK, NCSC
 * 
 * This file is part of HoneySpider Network 2.0.
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
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import mockit.Delegate;
import mockit.Mocked;
import mockit.NonStrict;
import mockit.NonStrictExpectations;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.gargoylesoftware.htmlunit.BrowserVersion;

import pl.nask.hsn2.ResourceException;
import pl.nask.hsn2.ServiceConnector;
import pl.nask.hsn2.StorageException;
import pl.nask.hsn2.bus.operations.ObjectData;
import pl.nask.hsn2.bus.operations.ObjectResponse;
import pl.nask.hsn2.bus.operations.builder.ObjectResponseBuilder;
import pl.nask.hsn2.protobuff.DataStore.DataResponse;
import pl.nask.hsn2.protobuff.DataStore.DataResponse.ResponseType;
import pl.nask.hsn2.protobuff.Object.Reference;
import pl.nask.hsn2.server.TestHttpServer;
import pl.nask.hsn2.service.task.WebClientTaskContext;
import pl.nask.hsn2.service.urlfollower.HtmlUnitFollower;
import pl.nask.hsn2.service.urlfollower.OutgoingLink;
import pl.nask.hsn2.service.urlfollower.PageLinks;
import pl.nask.hsn2.utils.MockTestsHelper;
import pl.nask.hsn2.utils.WebClientDataStoreHelper;

public class NonHttpProtocolTest {
	public static final String REFERRER = "http://referrer/referrer.html";
	@Mocked
	ServiceConnector connector;
	@NonStrict
	final WebClientDataStoreHelper wcHelper = null;

	WebClientTaskContext jobContext;
	ServiceParameters params;
	HtmlUnitFollower follower;
	WebClientTask webClientTask;
	String ftpPage1 = "ftp://page1";
	String ftpPage2 = "ftp://page2";
	boolean ftpPage1Found = false;
	boolean ftpPage2Found = false;

	private int newObjectsInOSCounter;
	private Set<String> newObjectStoreObjects;
	private Set<String> updatedAttributes;

	@BeforeClass
	public void startServer() throws Exception {
		TestHttpServer.startServer("nonHttpProtocol");
	}

	@AfterClass
	public void stopServer() throws Exception {
		TestHttpServer.stopServer();
	}

	@BeforeMethod
	public void beforeMethidInitContext() {
		jobContext = new WebClientTaskContext(1, 1, 1, connector);
		ftpPage1Found = ftpPage2Found = false;
		newObjectsInOSCounter = 0;
		newObjectStoreObjects = new TreeSet<>();
		updatedAttributes = new TreeSet<>();
	}

	@SuppressWarnings({ "unused", "rawtypes", "unchecked" })
	private void connectorExpectations() throws Exception {
		DataResponse.Builder responseBuilder = DataResponse.newBuilder();
		responseBuilder.setType(ResponseType.OK);
		responseBuilder.setRef(Reference.newBuilder().setKey(1L).setStore(1));
		final DataResponse dataResponse = responseBuilder.build();
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

				// Updating job context with new attributes.
				connector.updateObject(anyLong, withInstanceOf(ObjectData.class));
				result = saveObjectsResponse;
				forEachInvocation = new Object() {
					void validate(long jobId, ObjectData data) {
						MockTestsHelper.connectorUpdatesObject(data, updatedAttributes);
					}
				};
			}
		};
	}

	private void initJobContextAndParams(String testPageAbsoluteUrl) throws Exception {
		initJobContextAndParams(testPageAbsoluteUrl, "Default");
	}
	
	private void initJobContextAndParams(String testPageAbsoluteUrl,String browserProfile) throws Exception {
		long jobId = 1L;
		int reqId = 2;
		long objectDataId = 3L;
		long inputUrlId = 4L;
		long inputReferrerCookieId = 5L;
		int depth = 6;
		long topAncestorId = 7L;
		jobContext = new WebClientTaskContext(jobId, reqId, objectDataId, connector);
		params = new ServiceParameters();
		params.setProcessExternalLinks(0);
		params.setSaveHtml(false);
		params.setSaveCookies(false);
		final int longTimeout = 99999999;
		params.setPageTimeoutMillis(longTimeout);
		params.setProcessingTimeout(longTimeout);
		params.setProfile(browserProfile);
		ServiceData serviceData = new ServiceData(inputUrlId, testPageAbsoluteUrl, testPageAbsoluteUrl, REFERRER, inputReferrerCookieId, depth, topAncestorId, null);
		follower = new HtmlUnitFollower(testPageAbsoluteUrl, jobContext, params);
		webClientTask = new WebClientTask(jobContext, params, serviceData, follower);
	}

	@Test
	public void nonHttpResourcesOnFailedList() throws Exception, ResourceException, StorageException, IOException {
		connectorExpectations();
		initJobContextAndParams(TestHttpServer.absoluteUrl("nonHttpLinks.html"));

		params.setSaveImages(true);
		webClientTask.process();
		jobContext.flush();
		Assert.assertTrue(follower.isSuccessfull());
		Assert.assertNotNull(newObjectStoreObjects);
		Assert.assertEquals(newObjectsInOSCounter, 1);
		Assert.assertTrue(newObjectStoreObjects.contains("ftp://abc.dd.pl/;url;embedded;inactive"));
	}

	@Test
	public void nonHttpPageLinksOutgoingList() throws Exception {
		initJobContextAndParams(TestHttpServer.absoluteUrl("nonHttpLinks.html"));
		params.setProcessExternalLinks(1);
		webClientTask.process();
		Set<OutgoingLink> links = follower.getPageLinks().getOutgoingLinks();
		Assert.assertNotNull(links);
		Assert.assertEquals(links.size(), 1);
		Assert.assertTrue(MockTestsHelper.isOnOngoingLinksList(links, "skype:asdqwe?call"));
	}

	@Test
	public void nonHttpFrames() throws Exception {
		connectorExpectations();
		initJobContextAndParams(TestHttpServer.absoluteUrl("nonHttpFrames.html"),"Firefox 17");
		webClientTask.process();
		jobContext.flush();
		Assert.assertTrue(follower.isSuccessfull());
		Assert.assertNotNull(newObjectStoreObjects);
		Assert.assertEquals(newObjectsInOSCounter, 4);
		Assert.assertTrue(newObjectStoreObjects.contains("ftp://page1;url;frame"));
		Assert.assertTrue(newObjectStoreObjects.contains("ftp://page2;url;frame"));
		
		Assert.assertTrue(newObjectStoreObjects.contains("http://www-groups.dcs.st-and.ac.uk/history/BiogIndex.html;url;frame"));
		Assert.assertTrue(newObjectStoreObjects.contains("http://cdimage.ubuntu.com/;url;frame"));
		
	}

	@Test
	public void nonHttpIFrames() throws Exception {
		connectorExpectations();
		initJobContextAndParams(TestHttpServer.absoluteUrl("nonHttp_i_Frames.html"));
		webClientTask.process();
		jobContext.flush();
		Assert.assertTrue(follower.isSuccessfull());
		Assert.assertNotNull(newObjectStoreObjects);
		Assert.assertEquals(newObjectsInOSCounter, 4);
		Assert.assertTrue(newObjectStoreObjects.contains("ftp://page1;url;iframe"));
		Assert.assertTrue(newObjectStoreObjects.contains("ftp://page2;url;iframe"));
		
		Assert.assertTrue(newObjectStoreObjects.contains("http://www-groups.dcs.st-and.ac.uk/history/BiogIndex.html;url;iframe"));
		Assert.assertTrue(newObjectStoreObjects.contains("http://cdimage.ubuntu.com/;url;iframe"));
		
	}
	@Test(enabled = false)
	public void nonHttpRedirects1() throws Exception {
		connectorExpectations();
		initJobContextAndParams(TestHttpServer.absoluteUrl("nonHttpRedirects1.html"));
		webClientTask.process();
		jobContext.flush();
		Assert.assertEquals(ftpPage1Found && ftpPage2Found, true);
	}

	@Test(enabled = false)
	public void nonHttpRedirects2() throws Exception {
		connectorExpectations();
		params = new ServiceParameters();
		follower = new HtmlUnitFollower(TestHttpServer.absoluteUrl("nonHttpRedirects1.html"), jobContext, params);
		webClientTask = new WebClientTask(jobContext, params, new ServiceData("", ""), follower);
		webClientTask.process();
		jobContext.flush();
		Assert.assertEquals(ftpPage1Found && ftpPage2Found, true);
	}

	@Test(enabled = false)
	public void nonHttpRedirects3() throws Exception {
		connectorExpectations();
		params = new ServiceParameters();
		follower = new HtmlUnitFollower(TestHttpServer.absoluteUrl("nonHttpRedirects1.html"), jobContext, params);
		webClientTask = new WebClientTask(jobContext, params, new ServiceData("", ""), follower);
		webClientTask.process();
		jobContext.flush();
		Assert.assertEquals(ftpPage1Found && ftpPage2Found, true);
	}

	@Test
	public void illegalCharacterInUrlLink() throws Exception {
		new NonStrictExpectations() {
			@Mocked({ "logUriSyntaxError*" })
			PageLinks pageLinksExpectations;
			{
				invoke(pageLinksExpectations, "logUriSyntaxError", anyString, anyString, withAny(URISyntaxException.class));
				times = 1; // there is 1 broken outgoing link in HTML code
			}
		};
		connectorExpectations();
		initJobContextAndParams(TestHttpServer.absoluteUrl("nonHttpLinksIllegalCharacterInUrl.html"));
		params.setSaveImages(true);
		params.setSaveObjects(true);
		params.setProcessExternalLinks(1);
		webClientTask.process();
		jobContext.flush();

		Assert.assertTrue(follower.isSuccessfull());

		Set<OutgoingLink> outgoingLinks = follower.getPageLinks().getOutgoingLinks();
		Assert.assertNotNull(outgoingLinks);
		Assert.assertEquals(outgoingLinks.size(), 0);

		Assert.assertNotNull(newObjectStoreObjects);
		Assert.assertEquals(newObjectsInOSCounter, 3);
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("1^") + ";url;embedded;inactive"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("3^") + ";url;embedded;inactive"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("4^") + ";url;embedded;inactive"));
	}
}
