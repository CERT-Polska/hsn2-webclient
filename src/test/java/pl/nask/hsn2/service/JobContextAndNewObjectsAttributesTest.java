package pl.nask.hsn2.service;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import junit.framework.Assert;
import mockit.Delegate;
import mockit.Mocked;
import mockit.NonStrict;
import mockit.NonStrictExpectations;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import pl.nask.hsn2.ServiceConnector;
import pl.nask.hsn2.bus.operations.AttributeType;
import pl.nask.hsn2.bus.operations.ObjectData;
import pl.nask.hsn2.bus.operations.ObjectResponse;
import pl.nask.hsn2.bus.operations.builder.ObjectResponseBuilder;
import pl.nask.hsn2.connector.REST.DataResponse;
import pl.nask.hsn2.server.TestHttpServer;
import pl.nask.hsn2.service.task.WebClientTaskContext;
import pl.nask.hsn2.service.urlfollower.HtmlUnitFollower;
import pl.nask.hsn2.utils.MockTestsHelper;
import pl.nask.hsn2.utils.WebClientDataStoreHelper;

public class JobContextAndNewObjectsAttributesTest {
	@Mocked
	private ServiceConnector connector;
	@NonStrict
	WebClientDataStoreHelper helper = null;

	private ServiceParameters params;
	private HtmlUnitFollower follower;
	private WebClientTask webClientTask;
	private WebClientTaskContext jobContext;

	private Map<String, String> updatedAttributes;
	private Set<String> newObjectStoreObjects;
	private int newObjectsInOSCounter;
	private List<ObjectData> newObjectsSavedWithAttributes;

	@BeforeClass
	public void initServer() throws Exception {
		TestHttpServer.startServer("jobContextAndNewObjectsAttributes");
	}

	@AfterClass
	public void stopServer() throws Exception {
		TestHttpServer.stopServer();
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
						newObjectsSavedWithAttributes = dataList;
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
		newObjectStoreObjects = new TreeSet<>();
		updatedAttributes = new TreeMap<>();
		newObjectsInOSCounter = 0;

		long jobId = 1L;
		int reqId = 2;
		long objectDataId = 3L;
		long inputUrlId = 4L;
		long inputReferrerCookieId = 5L;
		int depth = 6;
		long topAncestorId = 7L;
		jobContext = new WebClientTaskContext(jobId, reqId, objectDataId, connector);
		params = new ServiceParameters();
		params.setProcessExternalLinks(false);
		params.setPageTimeoutMillis(999999);
		params.setSaveImages(true);
		ServiceData serviceData = new ServiceData(inputUrlId, testPageAbsoluteUrl, testPageAbsoluteUrl, MockTestsHelper.REFERRER, inputReferrerCookieId, depth, topAncestorId,null);
		follower = new HtmlUnitFollower(testPageAbsoluteUrl, jobContext, params);
		webClientTask = new WebClientTask(jobContext, params, serviceData, follower);
	}

	@Test
	public void jobUrlHostNotFound() throws Exception {
		defineConnectorExpectations();
		initJobContextAndParams("http://" + MockTestsHelper.NON_EXISTANT_HOST_NAME);
		webClientTask.process();
		jobContext.flush();

		Assert.assertEquals(newObjectsInOSCounter, 0);
		Assert.assertEquals(updatedAttributes.size(), 4);
		Assert.assertEquals(updatedAttributes.get("active"), "false");
		Assert.assertNotNull(updatedAttributes.get("http_request"));
		Assert.assertTrue(updatedAttributes.containsKey("download_time_start"));
		Assert.assertTrue(updatedAttributes.get("reason_failed").startsWith("java.net.UnknownHostException: " + MockTestsHelper.NON_EXISTANT_HOST_NAME));
	}

	@Test
	public void jobUrlFileNotFound404() throws Exception {
		defineConnectorExpectations();
		initJobContextAndParams(TestHttpServer.absoluteUrl("noSuchFile-123abc.txt"));
		webClientTask.process();
		jobContext.flush();

		Assert.assertEquals(newObjectsInOSCounter, 0);
		Assert.assertEquals(updatedAttributes.size(), 9);
		Assert.assertEquals(updatedAttributes.get("active"), "true");
		Assert.assertEquals(updatedAttributes.get("html"), "true");
		Assert.assertEquals(updatedAttributes.get("http_code"), "404");
		Assert.assertEquals(updatedAttributes.get("referrer"), MockTestsHelper.REFERRER);
		Assert.assertNotNull(updatedAttributes.get("html_source"));
		Assert.assertNotNull(updatedAttributes.get("http_request"));
		Assert.assertNotNull(updatedAttributes.get("cookie_list"));
		Assert.assertNotNull(updatedAttributes.get("download_time_start"));
		Assert.assertNotNull(updatedAttributes.get("download_time_end"));
	}

	@Test
	public void jobUrlWrongScheme() throws Exception {
		defineConnectorExpectations();
		initJobContextAndParams("ftp://localhost/");
		webClientTask.process();
		jobContext.flush();

		Assert.assertEquals(0, newObjectsInOSCounter);
		Assert.assertEquals(4, updatedAttributes.size());
		Assert.assertEquals(updatedAttributes.get("active"), "false");
		Assert.assertNotNull(updatedAttributes.get("http_request"));
		Assert.assertTrue(updatedAttributes.containsKey("download_time_start"));
		Assert.assertTrue(updatedAttributes.get("reason_failed").startsWith("org.apache.http.HttpException: Scheme 'ftp' not registered."));
	}

	@Test
	public void jobUrlConnectionTimeoutWrongPort() throws Exception {
		defineConnectorExpectations();
		initJobContextAndParams("http://localhost:12345/");
		webClientTask.process();
		jobContext.flush();

		Assert.assertEquals(0, newObjectsInOSCounter);
		Assert.assertEquals(4, updatedAttributes.size());
		Assert.assertEquals(updatedAttributes.get("active"), "false");
		Assert.assertNotNull(updatedAttributes.get("http_request"));
		Assert.assertTrue(updatedAttributes.get("reason_failed").startsWith("java.net.ConnectException:"));
		Assert.assertTrue(updatedAttributes.containsKey("download_time_start"));
	}

	@Test
	public void embeddedFileHostNotFound() throws Exception {
		defineConnectorExpectations();
		initJobContextAndParams(TestHttpServer.absoluteUrl("embeddedFileNoHost.html"));
		webClientTask.process();
		jobContext.flush();

		Assert.assertEquals(1, newObjectsSavedWithAttributes.size());
		Assert.assertNull(newObjectsSavedWithAttributes.get(0).findAttribute("NON_EXISTANT_ATTRIBUTE_NAME", AttributeType.BYTES));
		Assert.assertFalse(newObjectsSavedWithAttributes.get(0).findAttribute("active", AttributeType.BOOL).getBool());
		Assert.assertNotNull(newObjectsSavedWithAttributes.get(0).findAttribute("http_request", AttributeType.BYTES));
		Assert.assertEquals("embedded", newObjectsSavedWithAttributes.get(0).findAttribute("origin", AttributeType.STRING).getString());
		Assert.assertTrue(newObjectsSavedWithAttributes.get(0).findAttribute("reason_failed", AttributeType.STRING).getString()
				.startsWith("Host not found: noSuchHost-123abc"));
		Assert.assertEquals(TestHttpServer.absoluteUrl("embeddedFileNoHost.html"),
				newObjectsSavedWithAttributes.get(0).findAttribute("referrer", AttributeType.STRING).getString());
		Assert.assertNotNull(newObjectsSavedWithAttributes.get(0).findAttribute("referrer_cookie", AttributeType.BYTES));
		Assert.assertEquals("url", newObjectsSavedWithAttributes.get(0).findAttribute("type", AttributeType.STRING).getString());
		Assert.assertEquals("http://noSuchHost-123abc/img.png", newObjectsSavedWithAttributes.get(0).findAttribute("url_original", AttributeType.STRING)
				.getString());
		Assert.assertNotNull(newObjectsSavedWithAttributes.get(0).findAttribute("parent", AttributeType.OBJECT));
	}

	@Test
	public void embeddedFileFileNotFound404() throws Exception {
		defineConnectorExpectations();
		initJobContextAndParams(TestHttpServer.absoluteUrl("embeddedFileNoFile.html"));
		webClientTask.process();
		jobContext.flush();

		Assert.assertEquals(1, newObjectsSavedWithAttributes.size());
		Assert.assertNull(newObjectsSavedWithAttributes.get(0).findAttribute("NON_EXISTANT_ATTRIBUTE_NAME", AttributeType.BYTES));
		Assert.assertTrue(newObjectsSavedWithAttributes.get(0).findAttribute("active", AttributeType.BOOL).getBool());
		Assert.assertNotNull(newObjectsSavedWithAttributes.get(0).findAttribute("cookie_list", AttributeType.BYTES));
		Assert.assertTrue(newObjectsSavedWithAttributes.get(0).findAttribute("html", AttributeType.BOOL).getBool());
		Assert.assertNotNull(newObjectsSavedWithAttributes.get(0).findAttribute("html_source", AttributeType.BYTES));
		Assert.assertEquals(404, newObjectsSavedWithAttributes.get(0).findAttribute("http_code", AttributeType.INT).getInteger());
		Assert.assertNotNull(newObjectsSavedWithAttributes.get(0).findAttribute("http_request", AttributeType.BYTES));
		Assert.assertEquals("embedded", newObjectsSavedWithAttributes.get(0).findAttribute("origin", AttributeType.STRING).getString());
		Assert.assertNotNull(newObjectsSavedWithAttributes.get(0).findAttribute("parent", AttributeType.OBJECT));
		Assert.assertEquals(TestHttpServer.absoluteUrl("embeddedFileNoFile.html"),
				newObjectsSavedWithAttributes.get(0).findAttribute("referrer", AttributeType.STRING).getString());
		Assert.assertEquals("url", newObjectsSavedWithAttributes.get(0).findAttribute("type", AttributeType.STRING).getString());
		Assert.assertEquals(TestHttpServer.absoluteUrl("noSuchFile-123abc9z.png"),
				newObjectsSavedWithAttributes.get(0).findAttribute("url_original", AttributeType.STRING).getString());
	}

	@Test
	public void embeddedFileWrongScheme() throws Exception {
		defineConnectorExpectations();
		initJobContextAndParams(TestHttpServer.absoluteUrl("embeddedWrongScheme.html"));
		webClientTask.process();
		jobContext.flush();

		Assert.assertEquals(1, newObjectsSavedWithAttributes.size());
		Assert.assertNull(newObjectsSavedWithAttributes.get(0).findAttribute("NON_EXISTANT_ATTRIBUTE_NAME", AttributeType.BYTES));
		Assert.assertFalse(newObjectsSavedWithAttributes.get(0).findAttribute("active", AttributeType.BOOL).getBool());
		Assert.assertNotNull(newObjectsSavedWithAttributes.get(0).findAttribute("http_request", AttributeType.BYTES));
		Assert.assertEquals("embedded", newObjectsSavedWithAttributes.get(0).findAttribute("origin", AttributeType.STRING).getString());
		Assert.assertNotNull(newObjectsSavedWithAttributes.get(0).findAttribute("parent", AttributeType.OBJECT));
		Assert.assertEquals(TestHttpServer.absoluteUrl("embeddedWrongScheme.html"),
				newObjectsSavedWithAttributes.get(0).findAttribute("referrer", AttributeType.STRING).getString());
		Assert.assertNotNull(newObjectsSavedWithAttributes.get(0).findAttribute("referrer_cookie", AttributeType.BYTES));
		Assert.assertEquals("url", newObjectsSavedWithAttributes.get(0).findAttribute("type", AttributeType.STRING).getString());
		Assert.assertEquals("ftp://localhost/img.png", newObjectsSavedWithAttributes.get(0).findAttribute("url_original", AttributeType.STRING).getString());
	}

	@Test
	public void embeddedFileConnectionTimeoutWrongPort() throws Exception {
		defineConnectorExpectations();
		initJobContextAndParams(TestHttpServer.absoluteUrl("embeddedFileConnectionTimeoutWrongPort.html"));
		webClientTask.process();
		jobContext.flush();

		Assert.assertEquals(1, newObjectsSavedWithAttributes.size());
		Assert.assertNull(newObjectsSavedWithAttributes.get(0).findAttribute("NON_EXISTANT_ATTRIBUTE_NAME", AttributeType.BYTES));
		Assert.assertFalse(newObjectsSavedWithAttributes.get(0).findAttribute("active", AttributeType.BOOL).getBool());
		Assert.assertNotNull(newObjectsSavedWithAttributes.get(0).findAttribute("http_request", AttributeType.BYTES));
		Assert.assertEquals("embedded", newObjectsSavedWithAttributes.get(0).findAttribute("origin", AttributeType.STRING).getString());
		Assert.assertNotNull(newObjectsSavedWithAttributes.get(0).findAttribute("parent", AttributeType.OBJECT));
		Assert.assertEquals(TestHttpServer.absoluteUrl("embeddedFileConnectionTimeoutWrongPort.html"),
				newObjectsSavedWithAttributes.get(0).findAttribute("referrer", AttributeType.STRING).getString());
		Assert.assertNotNull(newObjectsSavedWithAttributes.get(0).findAttribute("referrer_cookie", AttributeType.BYTES));
		Assert.assertEquals("url", newObjectsSavedWithAttributes.get(0).findAttribute("type", AttributeType.STRING).getString());
		Assert.assertEquals("http://localhost:12345/img.png", newObjectsSavedWithAttributes.get(0).findAttribute("url_original", AttributeType.STRING)
				.getString());
	}
}
