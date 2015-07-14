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
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import mockit.Mocked;
import mockit.NonStrict;
import mockit.NonStrictExpectations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import pl.nask.hsn2.RequiredParameterMissingException;
import pl.nask.hsn2.ServiceConnector;
import pl.nask.hsn2.bus.operations.ObjectData;
import pl.nask.hsn2.bus.operations.ObjectResponse;
import pl.nask.hsn2.connector.REST.DataResponse;
import pl.nask.hsn2.server.TestHttpServer;
import pl.nask.hsn2.service.task.WebClientTaskContext;
import pl.nask.hsn2.service.urlfollower.HtmlUnitFollower;
import pl.nask.hsn2.utils.MockTestsHelper;
import pl.nask.hsn2.utils.WebClientDataStoreHelper;
import pl.nask.hsn2.wrappers.CookieWrapper;
import pl.nask.hsn2.wrappers.RequestWrapper;

public class FailedRequestTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(FailedRequestTest.class);
	public static final String REFERRER = "http://referrer/referrer.html";
	@Mocked
	private ServiceConnector connector;
	@NonStrict
	final WebClientDataStoreHelper wcHelper = null;
	private ServiceParameters params;
	private HtmlUnitFollower follower;
	private WebClientTask webClientTask;
	private WebClientTaskContext jobContext;
	private Map<String, String> updatedAttributes;
	private static final Set<CookieWrapper> COOKIE_WRAPPERS = new HashSet<CookieWrapper>();

	@BeforeClass
	private void initServer() throws Exception {
		TestHttpServer.startServer("pages");
	}

	@AfterClass
	private void stopServer() throws Exception {
		TestHttpServer.stopServer();
	}

	@SuppressWarnings("unused")
	private void defineTestsExpectations() throws Exception {
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

		new NonStrictExpectations() {
			final WebClientDataStoreHelper wcHelper = null;
			{
				// Get cookies from Data Store.
				WebClientDataStoreHelper.getCookiesFromDataStore(connector, anyLong, anyLong);
				result = COOKIE_WRAPPERS;
			}
		};
	}

	private void initJobContextAndParams(String testPageAbsoluteUrl) throws Exception {
		updatedAttributes = new TreeMap<>();

		jobContext = new WebClientTaskContext(1, 1, 1, connector);
		params = new ServiceParameters();
		ServiceData serviceData = new ServiceData(1l, testPageAbsoluteUrl, testPageAbsoluteUrl, REFERRER, 1l, 5, 1l, null);
		follower = new HtmlUnitFollower(testPageAbsoluteUrl, jobContext, params);
		webClientTask = new WebClientTask(jobContext, params, serviceData, follower);
	}

	@Test
	public void failedRequestTest() throws Exception {
		ServiceParameters params = new ServiceParameters();
		HtmlUnitFollower follower = new HtmlUnitFollower("http://localhost:8011/notHandled", new WebClientTaskContext(0, 0, 0, null), params);
		follower.processUrl();
		Assert.assertFalse(follower.isSuccessfull());
		Assert.assertNotNull(follower.getFailureMessage());
	}

	/**
	 * Host to crawl does not exists. WebClient should report crawling fail with
	 * "unknown host" fail message.
	 * 
	 * @throws Exception
	 */
	@Test
	public void unknownHostFailureMessageTest() throws Exception {
		String unknownHost = "hostUnknown12873612783162831";
		defineTestsExpectations();
		initJobContextAndParams("http://" + unknownHost);
		webClientTask.process();
		jobContext.flush();

		// Asserts.
		Assert.assertNotNull(updatedAttributes);
		Assert.assertEquals(updatedAttributes.get("active"), "false");
		String reasonFailed = updatedAttributes.get("reason_failed");
		Assert.assertNotNull(reasonFailed);
		Assert.assertTrue(reasonFailed.startsWith("java.net.UnknownHostException: " + unknownHost));
	}

	@Test(enabled=false)	// skipped due to problems to reach remote servers on build-server
	public void nonValidHttpFailureMessageTest() throws Exception {
		defineTestsExpectations();
		initJobContextAndParams("http://nask.pl:443/");
		webClientTask.process();
		jobContext.flush();

		// Asserts.
		Assert.assertNotNull(updatedAttributes);
		Assert.assertEquals(updatedAttributes.get("active"), "false");
		Assert.assertEquals(updatedAttributes.get("reason_failed"),
				"org.apache.http.ProtocolException: The server failed to respond with a valid HTTP response");
	}

	@Test
	public void htmlUnitFollowerRequestFailedTest() throws Exception {
		defineTestsExpectations();
		initJobContextAndParams("http://test");
		String msg1 = "requestFailedTestMessage1";
		String msg2 = "requestFailedTestMessage2";
		follower.requestFailed(msg1);
		Assert.assertEquals(follower.isSuccessfull(), false, "Follower should not be successfull.");
		Assert.assertEquals(follower.getFailureMessage(), msg1, "Failure message does not match.");
		follower.requestFailed("requestFailedTestMessage2");
		Assert.assertEquals(follower.getFailureMessage(), msg1 + "\n" + msg2, "Failure message does not match.");
		LOGGER.info(follower.getFailureMessage());
	}

	@Test
	public void requestWrapperValidation() {
		final String originalUrl = "http://orig-url-123";
		final String absoluteUrl = "http://abs-url-123";
		final String requestHeader = "request-header-test-123";
		try {
			new RequestWrapper(originalUrl, absoluteUrl, requestHeader);
		} catch (RequiredParameterMissingException e) {
			Assert.fail("RequestWrapper has not been created.");
		}
		try {
			new RequestWrapper(null, absoluteUrl, requestHeader);
			Assert.fail("RequestWrapper with null value has been created.");
		} catch (RequiredParameterMissingException e) {
		}
		try {
			new RequestWrapper(originalUrl, null, requestHeader);
			Assert.fail("RequestWrapper with null value has been created.");
		} catch (RequiredParameterMissingException e) {
		}
	}
}
