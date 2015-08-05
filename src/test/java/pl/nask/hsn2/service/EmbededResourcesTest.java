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

import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import mockit.Delegate;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import pl.nask.hsn2.ServiceConnector;
import pl.nask.hsn2.bus.operations.ObjectData;
import pl.nask.hsn2.bus.operations.ObjectResponse;
import pl.nask.hsn2.bus.operations.builder.ObjectResponseBuilder;
import pl.nask.hsn2.connector.REST.DataResponse;
import pl.nask.hsn2.server.TestHttpServer;
import pl.nask.hsn2.service.task.WebClientTaskContext;
import pl.nask.hsn2.service.urlfollower.HtmlUnitFollower;
import pl.nask.hsn2.utils.MockTestsHelper;
import pl.nask.hsn2.utils.WebClientDataStoreHelper;
import pl.nask.hsn2.wrappers.CookieWrapper;

import com.gargoylesoftware.htmlunit.javascript.JavaScriptEngine;

public class EmbededResourcesTest {
	private static final String FILE_FLAG = ";file";
	private static final String URL_EMBEDDED_FLAGS = ";url;embedded";
	private static final Set<CookieWrapper> COOKIE_WRAPPERS = new HashSet<CookieWrapper>();

	@Mocked
	private ServiceConnector connector;

	private ServiceParameters params;
	private HtmlUnitFollower follower;
	private WebClientTask webClientTask;
	private WebClientTaskContext jobContext;

	private int newObjectsInOSCounter;
	private Set<String> updatedAttributes;
	private Set<String> newObjectStoreObjects;

	@BeforeClass
	public void initServer() throws Exception {
		TestHttpServer.startServer("embeddedResources");
	}

	@AfterClass
	public void stopServer() throws Exception {
		TestHttpServer.stopServer();
	}

	/*
	 * Workaround to avoid problems between Cobertura and JMockit.
	 */
	@BeforeTest
	private final void initializeWebClientDataStoreHelper() {
		try {
			WebClientDataStoreHelper.getCookiesFromDataStore(null, 0, 0);
		} catch (Exception e) {
			// just ignore it
		}
	}

	private void initJobContextAndParams(String testPageAbsoluteUrl) throws Exception {
		updatedAttributes = new TreeSet<>();
		newObjectsInOSCounter = 0;
		newObjectStoreObjects = new TreeSet<>();

		jobContext = new WebClientTaskContext(1, 1, 1, connector);
		params = new ServiceParameters();
		params.setSaveHtml(false);
		params.setSaveCookies(false);
		params.setRedirectDepthLimit(10);
		params.setRedirectTotalLimit(50);
		ServiceData serviceData = new ServiceData(1l, testPageAbsoluteUrl, testPageAbsoluteUrl, MockTestsHelper.REFERRER, 1l, 5, 1l, null);
		follower = new HtmlUnitFollower(testPageAbsoluteUrl, jobContext, params);
		webClientTask = new WebClientTask(jobContext, params, serviceData, follower);

		// check if shutdownJavaScriptExecutor is invoked every in each case
		new MockUp<JavaScriptEngine>() {
			@Mock(minInvocations = 1)
			public void shutdownJavaScriptExecutor() {
			}
		};
	}

	@SuppressWarnings({ "unused", "rawtypes", "unchecked" })
	private void defineConnectorExpectations() throws Exception {
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

	@SuppressWarnings("unused")
	private void defineWebClientDataStoreHelperExpectations() throws Exception {
		new NonStrictExpectations() {
			WebClientDataStoreHelper wcHelper = null;
			{
				// Get cookies from Data Store.
				WebClientDataStoreHelper.getCookiesFromDataStore(connector, anyLong, anyLong);
				result = COOKIE_WRAPPERS;
			}
		};
	}

	@Test
	public void appletTest() throws Exception {
		// Initialize parameters and set any additional options.
		defineConnectorExpectations();
		defineWebClientDataStoreHelperExpectations();
		initJobContextAndParams(TestHttpServer.absoluteUrl("applet.html"));
		params.setSaveObjects(true);

		// Start crawling.
		webClientTask.process();
		jobContext.flush();

		// Asserts.
		Assert.assertEquals(newObjectsInOSCounter, 8);
		Assert.assertNotNull(newObjectStoreObjects);
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-01.txt") + URL_EMBEDDED_FLAGS));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-01.txt") + FILE_FLAG));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-02.txt") + URL_EMBEDDED_FLAGS));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-02.txt") + FILE_FLAG));
		Assert.assertTrue(newObjectStoreObjects.contains("http://127.0.0.1:" + TestHttpServer.getWebserverPort() + "/files/test-file-01.txt;url;embedded"));
		Assert.assertTrue(newObjectStoreObjects.contains("http://127.0.0.1:" + TestHttpServer.getWebserverPort() + "/files/test-file-01.txt;file"));
		Assert.assertTrue(newObjectStoreObjects.contains("http://127.0.0.1:" + TestHttpServer.getWebserverPort() + "/files/test-file-02.txt;url;embedded"));
		Assert.assertTrue(newObjectStoreObjects.contains("http://127.0.0.1:" + TestHttpServer.getWebserverPort() + "/files/test-file-02.txt;file"));
	}

	@Test
	public void appletTestWithSaveMultiple() throws Exception {
		// Initialize parameters and set any additional options.
		defineConnectorExpectations();
		defineWebClientDataStoreHelperExpectations();
		initJobContextAndParams(TestHttpServer.absoluteUrl("applet.html"));
		params.setSaveObjects(true);
		params.setSaveMultiple(true);

		// Start crawling.
		webClientTask.process();
		jobContext.flush();

		// Asserts.
		Assert.assertEquals(newObjectsInOSCounter, 12);
		// Note: 12 objects created, but only 8 different links
		// (4 links are the same as others).
		Assert.assertNotNull(newObjectStoreObjects);
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-01.txt") + URL_EMBEDDED_FLAGS));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-01.txt") + FILE_FLAG));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-02.txt") + URL_EMBEDDED_FLAGS));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-02.txt") + FILE_FLAG));
		Assert.assertTrue(newObjectStoreObjects.contains("http://127.0.0.1:" + TestHttpServer.getWebserverPort() + "/files/test-file-01.txt;url;embedded"));
		Assert.assertTrue(newObjectStoreObjects.contains("http://127.0.0.1:" + TestHttpServer.getWebserverPort() + "/files/test-file-01.txt;file"));
		Assert.assertTrue(newObjectStoreObjects.contains("http://127.0.0.1:" + TestHttpServer.getWebserverPort() + "/files/test-file-02.txt;url;embedded"));
		Assert.assertTrue(newObjectStoreObjects.contains("http://127.0.0.1:" + TestHttpServer.getWebserverPort() + "/files/test-file-02.txt;file"));
	}

	@Test
	public void audioSrcTest() throws Exception {
		// Initialize parameters and set any additional options.
		defineConnectorExpectations();
		defineWebClientDataStoreHelperExpectations();
		initJobContextAndParams(TestHttpServer.absoluteUrl("audioSrc.html"));
		// initJobContextAndParams(TestHttpServer.absoluteUrl("files/test1.ogg"));
		params.setSaveMultimedia(true);

		// Start crawling.
		webClientTask.process();
		jobContext.flush();

		// Asserts.
		Assert.assertEquals(newObjectsInOSCounter, 4);
		Assert.assertNotNull(newObjectStoreObjects);
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-01.txt") + FILE_FLAG));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-02.txt") + FILE_FLAG));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-01.txt") + URL_EMBEDDED_FLAGS));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-02.txt") + URL_EMBEDDED_FLAGS));
	}

	@Test
	public void bodyBackgroundTest() throws Exception {
		// Initialize parameters and set any additional options.
		defineConnectorExpectations();
		defineWebClientDataStoreHelperExpectations();
		initJobContextAndParams(TestHttpServer.absoluteUrl("bodyBackground.html"));
		params.setSaveImages(true);

		// Start crawling.
		webClientTask.process();
		jobContext.flush();

		// Asserts.
		Assert.assertEquals(newObjectsInOSCounter, 2);
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-01.txt") + FILE_FLAG));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-01.txt") + URL_EMBEDDED_FLAGS));
	}

	@Test
	public void commandIconTest() throws Exception {
		// Initialize parameters and set any additional options.
		defineConnectorExpectations();
		defineWebClientDataStoreHelperExpectations();
		initJobContextAndParams(TestHttpServer.absoluteUrl("commandIcon.html"));
		params.setSaveImages(true);

		// Start crawling.
		webClientTask.process();
		jobContext.flush();

		// Asserts.
		Assert.assertEquals(newObjectsInOSCounter, 4);
		Assert.assertNotNull(newObjectStoreObjects);
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-01.txt") + FILE_FLAG));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-01.txt") + URL_EMBEDDED_FLAGS));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-02.txt") + FILE_FLAG));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-02.txt") + URL_EMBEDDED_FLAGS));
	}

	@Test
	public void embedSrcTest() throws Exception {
		// Initialize parameters and set any additional options.
		defineConnectorExpectations();
		defineWebClientDataStoreHelperExpectations();
		initJobContextAndParams(TestHttpServer.absoluteUrl("embedSrc.html"));
		params.setSaveObjects(true);

		// Start crawling.
		webClientTask.process();
		jobContext.flush();

		// Asserts.
		Assert.assertEquals(newObjectsInOSCounter, 4);
		Assert.assertNotNull(newObjectStoreObjects);
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-01.txt") + FILE_FLAG));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-02.txt") + FILE_FLAG));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-01.txt") + URL_EMBEDDED_FLAGS));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-02.txt") + URL_EMBEDDED_FLAGS));
	}

	@Test
	public void htmlManifestAndCSSTest() throws Exception {
		// Initialize parameters and set any additional options.
		defineConnectorExpectations();
		defineWebClientDataStoreHelperExpectations();
		initJobContextAndParams(TestHttpServer.absoluteUrl("htmlManifest.html"));
		params.setSaveOthers(true);

		// Start crawling.
		webClientTask.process();
		jobContext.flush();

		// Asserts.
		Assert.assertEquals(newObjectsInOSCounter, 4);
		Assert.assertNotNull(newObjectStoreObjects);
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-01.txt") + FILE_FLAG));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-02.txt") + FILE_FLAG));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-01.txt") + URL_EMBEDDED_FLAGS));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-02.txt") + URL_EMBEDDED_FLAGS));
	}

	@Test(enabled=false)	// skipped due to problems to reach remote sites on build-server
	public void imgSrcAndFailedFilesTest() throws Exception {
		// Initialize parameters and set any additional options.
		defineConnectorExpectations();
		defineWebClientDataStoreHelperExpectations();
		initJobContextAndParams(TestHttpServer.absoluteUrl("imgSrc.html"));
		params.setSaveImages(true);

		// Start crawling.
		webClientTask.process();
		jobContext.flush();

		// Asserts.
		Assert.assertEquals(newObjectsInOSCounter, 12);
		Assert.assertNotNull(newObjectStoreObjects);
		Assert.assertTrue(newObjectStoreObjects.contains("http://commons.apache.org/images/logo.png;url;embedded"));
		Assert.assertTrue(newObjectStoreObjects.contains("http://commons.apache.org/images/logo.png;file"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-01.txt;url;embedded")));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-01.txt;file")));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-02.txt;url;embedded")));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-02.txt;file")));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("imageDoesNotExists.png;url;embedded;404")));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("movedContext/logo-nask1.gif;url;embedded")));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("newLocation/logo-nask1.gif;url;redirect")));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("newLocation/logo-nask1.gif;file")));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("newLocation/logo-nask.gif;url;embedded")));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("newLocation/logo-nask.gif;file")));
	}

	@Test
	public void inputSrcImageTest() throws Exception {
		// Initialize parameters and set any additional options.
		defineConnectorExpectations();
		defineWebClientDataStoreHelperExpectations();
		initJobContextAndParams(TestHttpServer.absoluteUrl("inputSrcImage.html"));
		params.setSaveImages(true);

		// Start crawling.
		webClientTask.process();
		jobContext.flush();

		// Asserts.
		Assert.assertEquals(newObjectsInOSCounter, 4);
		Assert.assertNotNull(newObjectStoreObjects);
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-01.txt") + URL_EMBEDDED_FLAGS));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-01.txt") + FILE_FLAG));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-02.txt") + URL_EMBEDDED_FLAGS));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-02.txt") + FILE_FLAG));
	}

	@Test
	public void linkHrefIconTest() throws Exception {
		// Initialize parameters and set any additional options.
		defineConnectorExpectations();
		defineWebClientDataStoreHelperExpectations();
		initJobContextAndParams(TestHttpServer.absoluteUrl("linkHrefIcon.html"));
		params.setSaveImages(true);

		// Start crawling.
		webClientTask.process();
		jobContext.flush();

		// Asserts.
		Assert.assertEquals(newObjectsInOSCounter, 2);
		Assert.assertNotNull(newObjectStoreObjects);
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-01.txt") + URL_EMBEDDED_FLAGS));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-01.txt") + FILE_FLAG));
	}

	@Test
	public void linkHrefStylesheetTest() throws Exception {
		// Initialize parameters and set any additional options.
		defineConnectorExpectations();
		defineWebClientDataStoreHelperExpectations();
		initJobContextAndParams(TestHttpServer.absoluteUrl("linkHrefStylesheet.html"));
		params.setSaveOthers(true);

		// Start crawling.
		webClientTask.process();
		jobContext.flush();

		// Asserts.
		Assert.assertEquals(newObjectsInOSCounter, 2);
		Assert.assertNotNull(newObjectStoreObjects);
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-01.txt") + URL_EMBEDDED_FLAGS));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-01.txt") + FILE_FLAG));
	}

	@Test
	public void objectTest() throws Exception {
		// Initialize parameters and set any additional options.
		defineConnectorExpectations();
		defineWebClientDataStoreHelperExpectations();
		initJobContextAndParams(TestHttpServer.absoluteUrl("object.html"));
		params.setSaveObjects(true);

		// Start crawling.
		webClientTask.process();
		jobContext.flush();

		// Asserts.
		Assert.assertEquals(newObjectsInOSCounter, 10);
		Assert.assertNotNull(newObjectStoreObjects);
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-01.txt") + URL_EMBEDDED_FLAGS));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-01.txt") + FILE_FLAG));
		String filesRootDir = "http://127.0.0.1:" + TestHttpServer.getWebserverPort() + "/files/";
		Assert.assertTrue(newObjectStoreObjects.contains(filesRootDir + "obj-file-01.txt" + URL_EMBEDDED_FLAGS));
		Assert.assertTrue(newObjectStoreObjects.contains(filesRootDir + "obj-file-01.txt" + FILE_FLAG));
		Assert.assertTrue(newObjectStoreObjects.contains(filesRootDir + "obj-file-02.txt" + URL_EMBEDDED_FLAGS));
		Assert.assertTrue(newObjectStoreObjects.contains(filesRootDir + "obj-file-02.txt" + FILE_FLAG));
		Assert.assertTrue(newObjectStoreObjects.contains(filesRootDir + "obj-file-03.txt" + URL_EMBEDDED_FLAGS));
		Assert.assertTrue(newObjectStoreObjects.contains(filesRootDir + "obj-file-03.txt" + FILE_FLAG));
		Assert.assertTrue(newObjectStoreObjects.contains(filesRootDir + "obj-file-04.txt" + URL_EMBEDDED_FLAGS));
		Assert.assertTrue(newObjectStoreObjects.contains(filesRootDir + "obj-file-04.txt" + FILE_FLAG));
	}

	@Test
	public void videoPosterAnsSrcTest() throws Exception {
		// Initialize parameters and set any additional options.
		defineConnectorExpectations();
		defineWebClientDataStoreHelperExpectations();
		initJobContextAndParams(TestHttpServer.absoluteUrl("videoPosterAndSource.html"));
		params.setSaveImages(true);
		params.setSaveMultimedia(true);

		// Start crawling.
		webClientTask.process();
		jobContext.flush();

		// Asserts.
		Assert.assertEquals(newObjectsInOSCounter, 4);
		Assert.assertNotNull(newObjectStoreObjects);
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-01.txt") + URL_EMBEDDED_FLAGS));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-01.txt") + FILE_FLAG));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-02.txt") + URL_EMBEDDED_FLAGS));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-02.txt") + FILE_FLAG));
	}

	@Test
	public void scriptSrcTest() throws Exception {
		// Initialize parameters and set any additional options.
		defineConnectorExpectations();
		defineWebClientDataStoreHelperExpectations();
		initJobContextAndParams(TestHttpServer.absoluteUrl("scriptSrc.html"));
		params.setSaveOthers(true);

		// Start crawling.
		webClientTask.process();
		jobContext.flush();

		// Asserts.
		Assert.assertEquals(newObjectsInOSCounter, 2);
		Assert.assertNotNull(newObjectStoreObjects);
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/jsSource.js") + URL_EMBEDDED_FLAGS));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/jsSource.js") + FILE_FLAG));
	}

	@Test
	public void sourceSrcTest() throws Exception {
		// Initialize parameters and set any additional options.
		defineConnectorExpectations();
		defineWebClientDataStoreHelperExpectations();
		initJobContextAndParams(TestHttpServer.absoluteUrl("sourceSrc.html"));
		params.setSaveMultimedia(true);

		// Start crawling.
		webClientTask.process();
		jobContext.flush();

		// Asserts.
		Assert.assertEquals(newObjectsInOSCounter, 4);
		Assert.assertNotNull(newObjectStoreObjects);
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-01.txt") + URL_EMBEDDED_FLAGS));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-01.txt") + FILE_FLAG));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-02.txt") + URL_EMBEDDED_FLAGS));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-02.txt") + FILE_FLAG));
	}

	@Test
	public void htmlBaseTest() throws Exception {
		// Initialize parameters and set any additional options.
		defineConnectorExpectations();
		defineWebClientDataStoreHelperExpectations();
		initJobContextAndParams(TestHttpServer.absoluteUrl("htmlBase.html"));
		params.setSaveImages(true);
		params.setSaveMultimedia(true);
		params.setSaveObjects(true);
		params.setSaveOthers(true);

		// Start crawling.
		webClientTask.process();
		jobContext.flush();

		// Asserts.
		Assert.assertEquals(newObjectsInOSCounter, 10);
		Assert.assertNotNull(newObjectStoreObjects);
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.getWebserverAddress() + "/files/test-file-01.txt;file"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.getWebserverAddress() + "/files/test-file-01.txt;url;embedded"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.getWebserverAddress() + "/newLocation/arakis-banner.swf;file"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.getWebserverAddress() + "/newLocation/arakis-banner.swf;url;embedded"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.getWebserverAddress() + "/newLocation/logo-nask.gif;file"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.getWebserverAddress() + "/newLocation/logo-nask.gif;url;embedded"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.getWebserverAddress() + "/newLocation/logo-nask1.gif;file"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.getWebserverAddress() + "/newLocation/logo-nask1.gif;url;embedded"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.getWebserverAddress() + "/newLocation/testFile1.4.pdf;file"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.getWebserverAddress() + "/newLocation/testFile1.4.pdf;url;embedded"));
	}

	@Test
	public void htmlBaseAfterRelativeElementTest() throws Exception {
		// Initialize parameters and set any additional options.
		defineConnectorExpectations();
		defineWebClientDataStoreHelperExpectations();
		initJobContextAndParams(TestHttpServer.absoluteUrl("htmlBaseAfterRelativeResource.html"));
		params.setSaveImages(true);

		// Start crawling.
		webClientTask.process();
		jobContext.flush();

		// Asserts.
		Assert.assertEquals(newObjectsInOSCounter, 4);
		Assert.assertNotNull(newObjectStoreObjects);
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-01.txt") + URL_EMBEDDED_FLAGS));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-01.txt") + FILE_FLAG));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-02.txt") + URL_EMBEDDED_FLAGS));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-02.txt") + FILE_FLAG));
	}

	@Test
	public void htmlBaseOutsideHeadTest() throws Exception {
		// Initialize parameters and set any additional options.
		defineConnectorExpectations();
		defineWebClientDataStoreHelperExpectations();
		initJobContextAndParams(TestHttpServer.absoluteUrl("htmlBaseOutsideOfHead.html"));
		params.setSaveImages(true);

		// Start crawling.
		webClientTask.process();
		jobContext.flush();

		// Asserts.
		Assert.assertEquals(newObjectsInOSCounter, 2);
		Assert.assertNotNull(newObjectStoreObjects);
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-01.txt") + URL_EMBEDDED_FLAGS));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/test-file-01.txt") + FILE_FLAG));
	}
}
