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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import mockit.Delegate;
import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import pl.nask.hsn2.NewUrlObject;
import pl.nask.hsn2.ParameterException;
import pl.nask.hsn2.ResourceException;
import pl.nask.hsn2.ServiceConnector;
import pl.nask.hsn2.StorageException;
import pl.nask.hsn2.bus.operations.ObjectData;
import pl.nask.hsn2.bus.operations.ObjectResponse;
import pl.nask.hsn2.bus.operations.builder.ObjectResponseBuilder;
import pl.nask.hsn2.connector.REST.DataResponse;
import pl.nask.hsn2.server.TestHttpServer;
import pl.nask.hsn2.service.task.WebClientTaskContext;
import pl.nask.hsn2.service.urlfollower.EmbeddedResource;
import pl.nask.hsn2.service.urlfollower.HtmlUnitFollower;
import pl.nask.hsn2.utils.MockTestsHelper;
import pl.nask.hsn2.utils.WebClientDataStoreHelper;
import pl.nask.hsn2.wrappers.FailedRequestWrapper;
import pl.nask.hsn2.wrappers.JSContextWrapper;
import pl.nask.hsn2.wrappers.RequestWrapper;

public class WebClientJobContextTest {
	@Mocked
	ServiceConnector connector;
	private WebClientTaskContext jobContext;
	private ServiceParameters params;
	private HtmlUnitFollower follower;
	private WebClientTask webClientTask;
	private StringBuilder saveFailedRequestsInDataStoreString;
	/**
	 * Javascript counter that is incremented during the test.
	 */
	private int jsSavedScriptCounter;
	/**
	 * Contains attributes names which has been updated during the test.
	 */
	private Set<String> updatedAttributes;
	/**
	 * Counter for new objects created during test. Should be reset to 0 before
	 * starting test.
	 */
	private int newObjectsInOSCounter;
	/**
	 * New object are stored here. Should be emptied before starting test.
	 */
	private Set<String> newObjectStoreObjects;

	@BeforeClass
	public void initServer() throws Exception {
		TestHttpServer.startServer("embededResJobContextTest");
	}

	@AfterClass
	public void stopServer() throws Exception {
		TestHttpServer.stopServer();
	}

	@BeforeMethod
	public void initContext() {
		jobContext = new WebClientTaskContext(1, 1, 1, connector);
	}

	@SuppressWarnings({ "rawtypes", "unused" })
	private void connectorExpectations() throws StorageException, ParameterException, ResourceException, IOException {
		final DataResponse dataResponse = new DataResponse(1L);
		final ObjectResponse saveObjectsResponse = new ObjectResponse(pl.nask.hsn2.bus.operations.ObjectResponse.ResponseType.SUCCESS_PUT);
		saveObjectsResponse.setObjects(Collections.singleton(1L));
		new NonStrictExpectations() {
			{
				// Sends data to Data Store as a byte array.
				connector.sendDataStoreData(anyLong, withInstanceOf(byte[].class));
				result = dataResponse;
				forEachInvocation = new Object() {
					void validate(long jobId, byte[] data) {
						MockTestsHelper.saveFailedRequestInDataStore(data, saveFailedRequestsInDataStoreString);
					}
				};

				// Sends data to Data Store as an input stream.
				connector.sendDataStoreData(anyLong, withInstanceOf(InputStream.class));
				result = dataResponse;

				// Saving new objects to Object Store.
				connector.saveObjects(anyLong, null);
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

	@SuppressWarnings({ "unused", "unchecked" })
	private void dataStoreHelperExpectations() throws Exception {
		new NonStrictExpectations() {
			WebClientDataStoreHelper wcdsh = null;
			{
				// Save Javascript in Data Store.
				WebClientDataStoreHelper.saveJSContextsInDataStore(connector, anyLong, withInstanceOf(List.class));
				forEachInvocation = new Object() {
					void validate(ServiceConnector conn, long jobId, List<JSContextWrapper> list) {
						jsSavedScriptCounter = MockTestsHelper.checkForJsContext(list);
					}
				};
			}
		};
	}

	private void jobInit(String url) throws ParameterException {
		jsSavedScriptCounter = 0;
		updatedAttributes = new TreeSet<>();
		saveFailedRequestsInDataStoreString = new StringBuilder();
		newObjectsInOSCounter = 0;
		newObjectStoreObjects = new TreeSet<>();

		params = new ServiceParameters();
		params.setProcessExternalLinks(0);
		params.setLinkLimit(100);
		params.setSaveHtml(false);
		follower = new HtmlUnitFollower(url, jobContext, params);
		webClientTask = new WebClientTask(jobContext, params, new ServiceData("", ""), follower);
	}

	@Test
	public void imagesTest() throws Exception {
		updatedAttributes = new TreeSet<>();
		// Test init.
		connectorExpectations();
		dataStoreHelperExpectations();
		jobInit(TestHttpServer.absoluteUrl("images.html"));
		params.setSaveImages(true);

		// Do test.
		webClientTask.process();
		jobContext.flush();

		// Check assertions.
		Assert.assertTrue(follower.isSuccessfull());

		// There's 7 links to images but 2 of them points
		// to the same file so list size should be 6.
		Assert.assertEquals(newObjectsInOSCounter, 14);
		Assert.assertNotNull(newObjectStoreObjects);
		Assert.assertTrue(newObjectStoreObjects.contains("http://www.w3.org/Icons/valid-xhtml10;url;embedded"));
		Assert.assertTrue(newObjectStoreObjects.contains("http://www.w3.org/Icons/valid-xhtml10;file"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/left.png") + ";url;embedded"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/left.png") + ";file"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/checker.png") + ";url;embedded"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/checker.png") + ";file"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/shadow.png") + ";url;embedded"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/shadow.png") + ";file"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/favicon.png") + ";url;embedded"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/favicon.png") + ";file"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/right.png") + ";url;embedded"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/right.png") + ";file"));

		Assert.assertNotNull(updatedAttributes);
		Assert.assertEquals(updatedAttributes.size(), 7);
		Assert.assertTrue(updatedAttributes.contains("active"));
		Assert.assertTrue(updatedAttributes.contains("cookie_list"));
		Assert.assertTrue(updatedAttributes.contains("html"));
		Assert.assertTrue(updatedAttributes.contains("http_code"));
		Assert.assertTrue(updatedAttributes.contains("http_request"));
		Assert.assertTrue(updatedAttributes.contains("download_time_start"));
		Assert.assertTrue(updatedAttributes.contains("download_time_end"));
	}

	@Test
	public void imagesWith404Test() throws Exception {
		// Test init.
		connectorExpectations();
		dataStoreHelperExpectations();
		jobInit(TestHttpServer.absoluteUrl("imagesWith404.html"));
		params.setSaveImages(true);
		params.setSaveFailed(true);

		// Do test.
		webClientTask.process();
		jobContext.flush();

		// Check assertions.
		Assert.assertTrue(follower.isSuccessfull());

		Assert.assertEquals(newObjectsInOSCounter, 4);
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/checker.png") + ";url;embedded"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/checker.png") + ";file"));
		Assert.assertTrue(newObjectStoreObjects.contains("http://noHost/noDirectoryFound/fileNotFound.png;url;embedded;inactive"));
		Assert.assertTrue(newObjectStoreObjects.contains(TestHttpServer.absoluteUrl("files/left2.png") + ";url;embedded;404"));

		Assert.assertNotNull(updatedAttributes);
		Assert.assertEquals(updatedAttributes.size(), 7);
		Assert.assertTrue(updatedAttributes.contains("active"));
		Assert.assertTrue(updatedAttributes.contains("cookie_list"));
		Assert.assertTrue(updatedAttributes.contains("html"));
		Assert.assertTrue(updatedAttributes.contains("http_code"));
		Assert.assertTrue(updatedAttributes.contains("http_request"));
		Assert.assertTrue(updatedAttributes.contains("download_time_start"));
		Assert.assertTrue(updatedAttributes.contains("download_time_end"));
	}

	@Test
	public void htmlAndCookieTest() throws Exception {
		// test init
		connectorExpectations();
		dataStoreHelperExpectations();
		jobInit(TestHttpServer.absoluteUrl("htmlOnly.html"));

		// do test
		webClientTask.process();
		jobContext.flush();

		// check assertions
		Assert.assertTrue(follower.isSuccessfull());

		Assert.assertNotNull(updatedAttributes);
		Assert.assertEquals(updatedAttributes.size(), 7);
		Assert.assertTrue(updatedAttributes.contains("active"));
		Assert.assertTrue(updatedAttributes.contains("cookie_list"));
		Assert.assertTrue(updatedAttributes.contains("html"));
		Assert.assertTrue(updatedAttributes.contains("http_code"));
		Assert.assertTrue(updatedAttributes.contains("http_request"));
		Assert.assertTrue(updatedAttributes.contains("download_time_start"));
		Assert.assertTrue(updatedAttributes.contains("download_time_end"));
	}

	@Test
	public void htmlSaveTest() throws Exception {
		// test init
		connectorExpectations();
		dataStoreHelperExpectations();
		jobInit(TestHttpServer.absoluteUrl("images.html"));
		params.setSaveHtml(true);
		params.setSaveImages(false);

		// do test
		webClientTask.process();
		jobContext.flush();

		// check assertions
		Assert.assertTrue(follower.isSuccessfull());

		Assert.assertNotNull(updatedAttributes);
		Assert.assertEquals(updatedAttributes.size(), 8);
		Assert.assertTrue(updatedAttributes.contains("active"));
		Assert.assertTrue(updatedAttributes.contains("cookie_list"));
		Assert.assertTrue(updatedAttributes.contains("html"));
		Assert.assertTrue(updatedAttributes.contains("http_code"));
		Assert.assertTrue(updatedAttributes.contains("http_request"));
		Assert.assertTrue(updatedAttributes.contains("html_source"));
		Assert.assertTrue(updatedAttributes.contains("download_time_start"));
		Assert.assertTrue(updatedAttributes.contains("download_time_end"));
	}

	@Test
	public void testOutgoingLinkLimitInMainCtx() throws Exception {
		connectorExpectations();
		params = new ServiceParameters();
		params.setLinkLimit(2);
		params.setProcessExternalLinks(1);
		jobContext.setServiceParams(params);

		jobContext.newObject(new NewUrlObject("http://localhost/", "url"));
		jobContext.newObject(new NewUrlObject("http://localhost/1", "url"));
		jobContext.newObject(new NewUrlObject("http://localhost/2", "url"));

		jobContext.flush();

		Assert.assertEquals(jobContext.getAddedObjects().size(), 2);
	}

	@Test
	public void testOutgoingLinkLimitInSubcontexts() throws Exception {
		connectorExpectations();
		params = new ServiceParameters();
		params.setLinkLimit(2);
		params.setProcessExternalLinks(1);
		jobContext.setServiceParams(params);
		jobContext.openSubContext();
		jobContext.newObject(new NewUrlObject("http://localhost/", "url"));
		jobContext.newObject(new NewUrlObject("http://localhost/1", "url"));
		jobContext.newObject(new NewUrlObject("http://localhost/2", "url"));
		jobContext.newObject(new NewUrlObject("http://localhost/3", "url"));
		jobContext.newObject(new NewUrlObject("http://localhost/4", "url"));
		jobContext.closeSubContext();

		jobContext.flush();
		// one subcontext and 2 outgoing links will be added as new objects
		Assert.assertEquals(jobContext.getAddedObjects().size(), 3);
	}

	/**
	 * Test case: Web site contains 3 javascripts but due to short timeout one
	 * of them is not processed. Even if some scripts are not processed by web
	 * client they should be saved (if service parameter says so).
	 * 
	 * @throws Exception
	 */
	@Test
	public void jsContextSaveWithTimeoutTest() throws Exception {
		// test init
		connectorExpectations();
		dataStoreHelperExpectations();
		jobInit(TestHttpServer.absoluteUrl("jsTest.html"));
		params.setSaveJsContext(true);
		params.setSaveOthers(true);
		params.setBackgroundJsTimeoutMillis(1000);
		params.setSingleJsTimeoutMillis(1000);

		// do test
		webClientTask.process();
		jobContext.flush();

		// check assertions
		Assert.assertTrue(follower.isSuccessfull());
		Assert.assertEquals(jsSavedScriptCounter, 3, "Number of saved scripts should be 3.");
	}

	@Test
	public void SaveFailedRequestsInDataStoreTest() throws Exception {
		connectorExpectations();
		saveFailedRequestsInDataStoreString = new StringBuilder();
		final String baseUrl = "http://testBaseUrl-h6sd345";
		final String path = "/testSourceAttrValue-js12ue3y";
		final String baseAndPath = baseUrl + path;
		final String testFailureMsg = "testFailureMessage-h3c0nf3zj";
		final String testFailureHeader = "testFailureHeader-b7dh46z0";
		final String testFailureReason = "testFailureReason-g63vd94fh";

		EmbeddedResource embeddedResource = new EmbeddedResource(baseUrl, path, testFailureMsg, true, testFailureHeader);
		RequestWrapper requestWrapper = new RequestWrapper(embeddedResource);
		List<RequestWrapper> requestWrappers = new ArrayList<RequestWrapper>();
		requestWrappers.add(requestWrapper);
		FailedRequestWrapper failedRequestWrapper = new FailedRequestWrapper(requestWrappers, testFailureReason);
		Set<FailedRequestWrapper> failedRequestWrappers = new HashSet<FailedRequestWrapper>();
		failedRequestWrappers.add(failedRequestWrapper);

		WebClientDataStoreHelper.saveFailedRequestsInDataStore(connector, 1L, failedRequestWrappers);
		Assert.assertTrue(saveFailedRequestsInDataStoreString.toString().contains((baseAndPath).subSequence(0, baseAndPath.length())), "Embedded resource ["
				+ baseAndPath + "] not found on failed list.");
		Assert.assertTrue(saveFailedRequestsInDataStoreString.toString().contains((testFailureReason).subSequence(0, testFailureReason.length())),
				"Test failure reason not saved properly.");
		Assert.assertTrue(saveFailedRequestsInDataStoreString.toString().contains((testFailureHeader).subSequence(0, testFailureHeader.length())),
				"Test failure header not saved properly.");
	}
}
