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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mockit.Mocked;
import mockit.NonStrict;
import mockit.NonStrictExpectations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import pl.nask.hsn2.ServiceConnector;
import pl.nask.hsn2.bus.operations.Attribute;
import pl.nask.hsn2.bus.operations.AttributeType;
import pl.nask.hsn2.bus.operations.ObjectData;
import pl.nask.hsn2.bus.operations.ObjectResponse;
import pl.nask.hsn2.protobuff.DataStore.DataResponse;
import pl.nask.hsn2.protobuff.DataStore.DataResponse.ResponseType;
import pl.nask.hsn2.protobuff.Object.Reference;
import pl.nask.hsn2.server.TestHttpServer;
import pl.nask.hsn2.service.task.WebClientTaskContext;
import pl.nask.hsn2.service.urlfollower.HtmlUnitFollower;
import pl.nask.hsn2.service.urlfollower.OutgoingLink;
import pl.nask.hsn2.service.urlfollower.ScriptInterceptor.ScriptElement;
import pl.nask.hsn2.task.ObjectTreeNode;
import pl.nask.hsn2.utils.WebClientDataStoreHelper;
import pl.nask.hsn2.wrappers.CookieWrapper;
import pl.nask.hsn2.wrappers.FailedRequestWrapper;
import pl.nask.hsn2.wrappers.FileWrapper;
import pl.nask.hsn2.wrappers.RequestWrapper;

/**
 * This is an old test suite which used tomcat as a web server - it now uses
 * TestHttpServer and may contain tests, which are already implemented in other
 * suites
 */
public class FollowerWithCargoTomcatTest {
	private static final Logger LOGGER = LoggerFactory.getLogger("FollowerWithCargoTomcatTest");
	public static final String REFERRER = "http://referrer/referrer.html";
	private static final Set<CookieWrapper> COOKIE_WRAPPERS = new HashSet<CookieWrapper>();
	
	@Mocked
	private ServiceConnector connector;
	@NonStrict
	final WebClientDataStoreHelper wcHelper = null;
	
	private ServiceParameters params;
	private HtmlUnitFollower follower;
	private WebClientTask webClientTask;
	private WebClientTaskContext jobContext;
	private List<String> embeddedFilesList;
	private List<String> embeddedFailedFilesList;
	private List<String> savedObjects;

	@BeforeClass
	private void initServer() throws Exception {
		TestHttpServer.startServer("pages");
	}

	@AfterClass
	private void stopServer() throws Exception {
		TestHttpServer.stopServer();
	}

	private void initJobContextAndParams(String testPageAbsoluteUrl) throws Exception {
		embeddedFilesList = new ArrayList<String>();
		embeddedFailedFilesList = new ArrayList<String>();
		savedObjects = new ArrayList<String>();
		jobContext = new WebClientTaskContext(1, 1, 1, connector);
		params = new ServiceParameters();
		ServiceData serviceData = new ServiceData(1l, testPageAbsoluteUrl, testPageAbsoluteUrl, REFERRER, 1l, 5, 1l);
		follower = new HtmlUnitFollower(testPageAbsoluteUrl, jobContext, params);
		webClientTask = new WebClientTask(jobContext, params, serviceData, follower);
	}

	private void checkForAttributes(List<ObjectData> dataList) {
		for (ObjectData objectData : dataList) {
			LOGGER.info("Saving objects:");
			Set<Attribute> attrs = objectData.getAttributes();
			for (Attribute attribute : attrs) {
				String nameAndValue = attribute.getName() + "=" + attribute.getValue();
				savedObjects.add(nameAndValue);
				LOGGER.info("--> " + nameAndValue);
			}
		}
	}

	private void checkForAttributes(ObjectData data) {
		LOGGER.info("Updating object:");
		Set<Attribute> attrs = data.getAttributes();
		for (Attribute attribute : attrs) {
			String nameAndValue = attribute.getName() + "=" + attribute.getValue();
			LOGGER.info("--> " + nameAndValue);
		}
	}

	private void checkForEmbeddedRes(Set<FileWrapper> set) {
		for (FileWrapper fileWrapper : set) {
			List<RequestWrapper> requestWrappers = fileWrapper.getRequestWrappers();
			for (RequestWrapper requestWrapper : requestWrappers) {
				LOGGER.info("Embedded resource:");
				LOGGER.info("--> Orig url: " + requestWrapper.getOriginalUrl());
				LOGGER.info("--> Abs url: " + requestWrapper.getAbsoluteUrl());
				embeddedFilesList.add(requestWrapper.getAbsoluteUrl());
			}
		}
	}

	private void checkForFailedEmbeddedRes(Set<FailedRequestWrapper> set) {
		for (FailedRequestWrapper failedRequestWrapper : set) {
			List<RequestWrapper> requestWrappers = failedRequestWrapper.getRequestWrappers();
			for (RequestWrapper requestWrapper : requestWrappers) {
				LOGGER.info("Embedded resource failed:");
				LOGGER.info("--> Orig url: " + requestWrapper.getOriginalUrl());
				LOGGER.info("--> Abs url: " + requestWrapper.getAbsoluteUrl());
				embeddedFailedFilesList.add(requestWrapper.getAbsoluteUrl());
			}
		}
	}

	private void checkForCookies(Set<CookieWrapper> set) {
		LOGGER.info("Cookie:");
		for (CookieWrapper cookieWrapper : set) {
			Map<String, String> attributes = cookieWrapper.getAttributes();
			LOGGER.info("--> Name: " + cookieWrapper.getName());
			LOGGER.info("--> Value: " + cookieWrapper.getValue());
			LOGGER.info("--> Attributes: " + attributes);
		}
	}

	private void assertForOutgoingLinks(Set<OutgoingLink> list, String query) {
		for (OutgoingLink outgoingLink : list) {
			if (outgoingLink.getAbsoluteUrl().equalsIgnoreCase(query)) {
				return;
			}
		}
		Assert.fail("Outgoing link " + query + " in not found " + list);
	}

	@SuppressWarnings("unchecked")
	private void defineTestsExpectations() throws Exception {
		DataResponse.Builder responseBuilder = DataResponse.newBuilder();
		responseBuilder.setType(ResponseType.OK);
		responseBuilder.setRef(Reference.newBuilder().setKey(1L).setStore(1));
		final DataResponse dataResponse = responseBuilder.build();
		final ObjectResponse saveObjectsResponse = new ObjectResponse(pl.nask.hsn2.bus.operations.ObjectResponse.ResponseType.SUCCESS_PUT);
		saveObjectsResponse.setObjects(Collections.singleton(1L));
		new NonStrictExpectations() {
			{
				connector.sendDataStoreData(anyLong, withInstanceOf(byte[].class));
				result = dataResponse;
				connector.sendDataStoreData(anyLong, withInstanceOf(InputStream.class));
				result = dataResponse;
				connector.saveObjects(anyLong, null);
				result = saveObjectsResponse;
				forEachInvocation = new Object() {
					@SuppressWarnings("unused")
					void validate(long jobId, List<ObjectData> dataList) {
						checkForAttributes(dataList);
					}
				};
				connector.updateObject(anyLong, withInstanceOf(ObjectData.class));
				result = saveObjectsResponse;
				forEachInvocation = new Object() {
					@SuppressWarnings("unused")
					void validate(long jobId, ObjectData data) {
						checkForAttributes(data);
					}
				};
			}
		};

		new NonStrictExpectations() {
			@SuppressWarnings("unused")
			final WebClientDataStoreHelper wcHelper = null;
			{
				WebClientDataStoreHelper.getCookiesFromDataStore(connector, anyLong, anyLong);
				result = COOKIE_WRAPPERS;
				WebClientDataStoreHelper.saveInDataStore(connector, anyLong, withInstanceOf(Set.class));
				forEachInvocation = new Object() {
					@SuppressWarnings("unused")
					void validate(ServiceConnector conn, long jobId, Set<FileWrapper> set) {
						checkForEmbeddedRes(set);
					}
				};
				WebClientDataStoreHelper.saveFailedRequestsInDataStore(connector, anyLong, withInstanceOf(Set.class));
				forEachInvocation = new Object() {
					@SuppressWarnings("unused")
					void validate(ServiceConnector conn, long jobId, Set<FailedRequestWrapper> set) {
						checkForFailedEmbeddedRes(set);
					}
				};
				WebClientDataStoreHelper.saveCookiesInDataStore(connector, anyLong, withInstanceOf(Set.class));
				forEachInvocation = new Object() {
					@SuppressWarnings("unused")
					void validate(ServiceConnector conn, long jobId, Set<CookieWrapper> set) {
						checkForCookies(set);
					}
				};
			}
		};
	}

	private void openCloseContextExpectations() throws Exception {
		new NonStrictExpectations() {
			@Mocked({ "openSubContext", "closeSubContext" })
			WebClientTaskContext ctx;
			{
				ctx.openSubContext();
				minTimes = 1;
				ctx.closeSubContext();
				minTimes = 1;
			}
		};
	}

	@Test
	public void basicTest() throws Exception {
		// Initialize parameters and set any additional options.
		defineTestsExpectations();
		String testPageAbsoluteUrl = TestHttpServer.absoluteUrl("start.html");
		initJobContextAndParams(testPageAbsoluteUrl);

		// Start crawling.
		webClientTask.process();

		// Asserts.
		Assert.assertTrue(follower.isSuccessfull(), "isSuccessfull");
		Assert.assertNull(follower.getFailureMessage(), "failure message should be null");
		Assert.assertEquals(follower.getProcessedPage().getResponseCode(), 200, "response code");
		Assert.assertTrue(follower.getProcessedPage().isHtml(), "isHtml");
		Assert.assertNotNull(jobContext.getContextByUrl(testPageAbsoluteUrl).getObjectDataBuilder().build().findAttribute("html_source", AttributeType.BYTES), "no html_source ref attribute");
	}

	@Test
	public void outgoingLinksTest() throws Exception {
		// Initialize parameters and set any additional options.
		defineTestsExpectations();
		initJobContextAndParams(TestHttpServer.absoluteUrl("start.html"));

		// Start crawling.
		webClientTask.process();
		jobContext.flush();

		// Asserts.
		Set<OutgoingLink> linkList = jobContext.getPageLinks().getOutgoingLinks();
		Assert.assertEquals(linkList.size(), 4);
		assertForOutgoingLinks(linkList, TestHttpServer.absoluteUrl("metaRedirect.jsp"));
		assertForOutgoingLinks(linkList, TestHttpServer.absoluteUrl("page1.jsp"));
		assertForOutgoingLinks(linkList, TestHttpServer.absoluteUrl("page2.jsp"));
		assertForOutgoingLinks(linkList, "http://www.nask.pl");
	}

	@Test
	public void failedRequestTest() throws Exception {
		// Initialize parameters and set any additional options.
		defineTestsExpectations();
		initJobContextAndParams("http://123.123.123.123/failedTest.html");
		params.setPageTimeoutMillis(1000);

		// Start crawling.
		webClientTask.process();

		// Asserts.
		Assert.assertFalse(follower.isSuccessfull());
		Assert.assertNotNull(follower.getFailureMessage());
	}

	@Test(enabled = true)
	public void hiddenRedirectTest() throws Exception {
		// Initialize parameters and set any additional options.
		defineTestsExpectations();
		initJobContextAndParams(TestHttpServer.absoluteUrl("metaRedirect.html"));
		params.setPageTimeoutMillis(1000);

		// Start crawling.
		webClientTask.process();
		
		// Asserts.
		ObjectTreeNode childNode = jobContext.getContextByUrl(TestHttpServer.absoluteUrl("start.html"));
		Assert.assertNotNull(childNode);
		Assert.assertEquals(childNode.getParent(), jobContext.getContextByUrl(TestHttpServer.absoluteUrl("metaRedirect.html")));
	}

	@Test
	public void cookiesTest() throws Exception {
		// Initialize parameters and set any additional options.
		defineTestsExpectations();
		initJobContextAndParams(TestHttpServer.absoluteUrl("start.html"));

		// Start crawling.
		webClientTask.process();

		// Asserts.
		Set<CookieWrapper> cookieWrappers = follower.getCookies();
		Assert.assertEquals(cookieWrappers.size(), 1, "Cookies counter error.");
		for (CookieWrapper wrapper : cookieWrappers) {
			Assert.assertEquals(wrapper.getName(), "JSESSIONID");
		}
	}

	@Test
	public void framesTest() throws Exception {
		// Initialize parameters and set any additional options.
		defineTestsExpectations();
		openCloseContextExpectations();
		initJobContextAndParams(TestHttpServer.absoluteUrl("start.html"));

		// Start crawling.
		webClientTask.process();
	}

	@Test
	public void jsSaveTest() throws Exception {
		// Initialize parameters and set any additional options.
		defineTestsExpectations();
		String absoluteUrl = TestHttpServer.absoluteUrl("jsSave.html");
		initJobContextAndParams(absoluteUrl);
		params.setSaveJsContext(true);

		// Start crawling.
		webClientTask.process();

		// Asserts
		Map<String, Map<String, ScriptElement>>  scriptsByOrigin = jobContext.getLaunchedScriptsByOrigin();
		
		
		Assert.assertEquals(scriptsByOrigin.size(), 1);
		int evalCount = 0;
		Set<Integer> scriptIds = new HashSet<Integer>();
		for (Map<String, ScriptElement> scriptByUrl : scriptsByOrigin.values()) {
			for(ScriptElement script : scriptByUrl.values()){
				Assert.assertNotNull(script.getSource());
				if (script.isEval()) {
					evalCount++;
				}
				if (!scriptIds.add(script.getId())) {
					Assert.fail("Script id is expected to be unique!");
				}
			}
		}
		Assert.assertEquals(evalCount, 3);
	}

	@Test
	public void jsRedirectDocDOMFrameTest() throws Exception {
		// Initialize parameters and set any additional options.
		defineTestsExpectations();
		openCloseContextExpectations();
		initJobContextAndParams(TestHttpServer.absoluteUrl("jsRedirectDocDOMIframe.html"));

		// Start crawling.
		webClientTask.process();
	}

	@Test
	public void jsRedirectDocIframeTest() throws Exception {
		// Initialize parameters and set any additional options.
		defineTestsExpectations();
		openCloseContextExpectations();
		initJobContextAndParams(TestHttpServer.absoluteUrl("jsRedirectDocIframe.html"));

		// Start crawling.
		webClientTask.process();
	}

	@Test
    public void jsRedirectSelfLocationTest() throws Exception {
		defineTestsExpectations();
		openCloseContextExpectations();
		initJobContextAndParams(TestHttpServer.absoluteUrl("jsRedirectSelfLocation.html"));
		webClientTask.process();
	}

	@Test
	public void framesWithoutSrcTest() throws Exception {
		defineTestsExpectations();
		initJobContextAndParams(TestHttpServer.absoluteUrl("framesWithoutSrc.html"));
		webClientTask.process();
		jobContext.flush();

		// Asserts.
		int counter = 0;
		for (String s : savedObjects) {
			if (s.equals("url_original=about:blank")) {
				counter++;
			}
		}
		Assert.assertEquals(counter, 2, "There should be two about:blank frames saved.");
	}

	@Test
	public void htmlUnitFollowerGetOrigUrlTest() throws Exception {
		defineTestsExpectations();
		String fakeUrlForTest = "http://url.for.test/";
		initJobContextAndParams(fakeUrlForTest );
		Assert.assertEquals(follower.getOriginalUrl(), fakeUrlForTest);
	}
}
