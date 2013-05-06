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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.NonStrict;
import mockit.NonStrictExpectations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import pl.nask.hsn2.ParameterException;
import pl.nask.hsn2.ResourceException;
import pl.nask.hsn2.ServiceConnector;
import pl.nask.hsn2.StorageException;
import pl.nask.hsn2.bus.operations.Attribute;
import pl.nask.hsn2.bus.operations.ObjectData;
import pl.nask.hsn2.bus.operations.ObjectResponse;
import pl.nask.hsn2.connector.REST.DataResponse;
import pl.nask.hsn2.server.TestHttpServer;
import pl.nask.hsn2.service.task.WebClientTaskContext;
import pl.nask.hsn2.service.urlfollower.HtmlUnitFollower;
import pl.nask.hsn2.utils.WebClientDataStoreHelper;
import pl.nask.hsn2.wrappers.CookieWrapper;
import pl.nask.hsn2.wrappers.FailedRequestWrapper;
import pl.nask.hsn2.wrappers.FileWrapper;
import pl.nask.hsn2.wrappers.RequestWrapper;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.javascript.JavaScriptEngine;

public class AllSitesTest {
	private static final Logger LOGGER = LoggerFactory.getLogger("AllSitesTest");
	public static final String REFERRER = "http://referrer/referrer.html";
	public static final char SYSTEM_FILES_SEPARATOR = File.separatorChar;
	private static final Set<CookieWrapper> COOKIE_WRAPPERS = new HashSet<CookieWrapper>();
	
	private static final int BG_JS_TIMEOUT = 2 * 1000;
	private static final int SINGLE_JS_TIMEOUT = 1000;
	private static final int PAGE_TIMEOUT = 3 * 1000;
	private static final int DEFAULT_TEST_TIMEOUT = PAGE_TIMEOUT + SINGLE_JS_TIMEOUT + BG_JS_TIMEOUT + 1000;	
	
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
	private String webserverRootDirectory;
	private Set<String> testWebsitesRelativeUrls;
	
	private static Set<String> skippedTestSites = new HashSet<String>(
			Arrays.asList(
					//"jsTests/js5.html",
					""
			));

	@BeforeMethod
	public void prepareMock() {
		// check if shutdownJavaScriptExecutor is invoked every in each case
		new MockUp<JavaScriptEngine>() {	
			@Mock(minInvocations=1)
			public void shutdownJavaScriptExecutor(){}
		};
	}
	
	@BeforeClass
	public void initServer() throws Exception {
		TestHttpServer.startServer("");
	}

	@AfterClass
	public void stopServer() throws Exception {
		TestHttpServer.stopServer();
	}

	private void checkForAttributes(List<ObjectData> dataList) {
		StringBuilder sb = new StringBuilder();
		for (ObjectData objectData : dataList) {
			sb.append("\n\nSaving objects:\n");
			Set<Attribute> attrs = objectData.getAttributes();
			Set<String> attrs2 = new TreeSet<String>();
			for (Attribute attribute : attrs) {
				attrs2.add(attribute.getName() + "=" + attribute.getValue());
			}
			for (String attribute : attrs2) {
				sb.append("--> ").append(attribute).append("\n");
			}
		}
		LOGGER.info(sb.toString());
	}

	private void checkForAttributes(ObjectData data) {
		StringBuilder sb = new StringBuilder("\n\nUpdating objects:\n");
		Set<Attribute> attrs = data.getAttributes();
		Set<String> attrs2 = new TreeSet<String>();
		for (Attribute attribute : attrs) {
			attrs2.add(attribute.getName() + "=" + attribute.getValue());
		}
		for (String attribute : attrs2) {
			sb.append("--> ").append(attribute).append("\n");
		}
		LOGGER.info(sb.toString());
	}

	private void checkForEmbeddedRes(Set<FileWrapper> set) {
		StringBuilder sb = new StringBuilder();
		for (FileWrapper fileWrapper : set) {
			List<RequestWrapper> requestWrappers = fileWrapper.getRequestWrappers();
			sb.append("\n\nEmbedded resource:\n");
			for (RequestWrapper requestWrapper : requestWrappers) {
				sb.append("--> AbsUrl=").append(requestWrapper.getAbsoluteUrl()).append(", OrgUrl=").append(requestWrapper.getOriginalUrl()).append("\n");
				embeddedFilesList.add(requestWrapper.getAbsoluteUrl());
			}
		}
		LOGGER.info(sb.toString());
	}

	private void checkForFailedEmbeddedRes(Set<FailedRequestWrapper> set) {
		StringBuilder sb = new StringBuilder();
		for (FailedRequestWrapper failedFileWrapper : set) {
			List<RequestWrapper> requestWrappers = failedFileWrapper.getRequestWrappers();
			sb.append("\n\nFailed embedded resource:\n");
			for (RequestWrapper requestWrapper : requestWrappers) {
				sb.append("--> AbsUrl=").append(requestWrapper.getAbsoluteUrl()).append(", OrgUrl=").append(requestWrapper.getOriginalUrl()).append("\n");
				embeddedFailedFilesList.add(requestWrapper.getAbsoluteUrl());
			}
		}
		LOGGER.info(sb.toString());
	}

	@SuppressWarnings("unchecked")
	private void defineTestsExpectations() throws Exception {
		final DataResponse dataResponse = new DataResponse(1L);
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
			}
		};
	}

	private void initJobContextAndParams(String testPageAbsoluteUrl) throws Exception {
		embeddedFilesList = new ArrayList<String>();
		embeddedFailedFilesList = new ArrayList<String>();
		jobContext = new WebClientTaskContext(1, 1, 1, connector);
		params = new ServiceParameters();
		params.setSaveHtml(false);
		params.setSaveCookies(false);
		params.setPageTimeoutMillis(PAGE_TIMEOUT);
		params.setSingleJsTimeoutMillis(SINGLE_JS_TIMEOUT);
		params.setBackgroundJsTimeoutMillis(BG_JS_TIMEOUT);
		params.setSaveJsContext(true);
		params.setProcessExternalLinks(0);
		ServiceData serviceData = new ServiceData(1l, testPageAbsoluteUrl, testPageAbsoluteUrl, REFERRER, 1l, 5, 1l, null);
		follower = new HtmlUnitFollower(testPageAbsoluteUrl, jobContext, params);
		webClientTask = new WebClientTask(jobContext, params, serviceData, follower);
	}

	private void listFilesWithinSubdirs(String dirname, int depth) throws IOException {
		File dir = new File(dirname);
		if (dir != null && dir.isDirectory()) {
			for (String filename : dir.list()) {
				File file = new File(dirname + SYSTEM_FILES_SEPARATOR + filename);
				if (file.isDirectory()) {
					listFilesWithinSubdirs(file.toString(), depth + 1);
				} else if (filename.endsWith(".html")) {
					String absName = file.getAbsolutePath();

					if (absName.startsWith(webserverRootDirectory) && absName.length() > webserverRootDirectory.length() + 1) {
						String relativeUrlNormalized = absName.substring(webserverRootDirectory.length() + 1);
						relativeUrlNormalized = relativeUrlNormalized.replace(SYSTEM_FILES_SEPARATOR, '/');
						testWebsitesRelativeUrls.add(relativeUrlNormalized);
						LOGGER.debug("Added: " + relativeUrlNormalized);
					} else {
						LOGGER.debug("ERROR: " + absName);
					}

				}
			}
		} else {
			LOGGER.error("Directory {} does not exist", dir);
		}
	}

	public void prepareHtmlFilesForTest() throws Exception {
		testWebsitesRelativeUrls = new TreeSet<String>();
		URL testDirUri = AllSitesTest.class.getResource("/webtest");
		webserverRootDirectory = testDirUri.getPath();
		if (webserverRootDirectory.charAt(2) == ':') {
			// Test is running on Windows platform and we need to trim leading
			// slash
			webserverRootDirectory = webserverRootDirectory.substring(1);
		}
		webserverRootDirectory = webserverRootDirectory.replace('/', SYSTEM_FILES_SEPARATOR);
		if (testDirUri != null) {
			listFilesWithinSubdirs(webserverRootDirectory, 0);
		}
	}

	@DataProvider(name = "websitesToTest")
	public Object[][] prepareDataProvider() throws Exception {
		prepareHtmlFilesForTest();
		int sitesCount = testWebsitesRelativeUrls.size();
		List<Object[]> websites = new ArrayList<Object[]>(sitesCount);
		for (String relUrl : testWebsitesRelativeUrls) {
			if (!skippedTestSites.contains(relUrl)) {
				Object[] site = { relUrl };
				websites.add(site);
			} else {
				LOGGER.info("Removing {} from test sites suite", relUrl);
			}
		}
		
		return websites.toArray(new Object[0][0]);
	}
	
	@Test(dataProvider = "websitesToTest", timeOut=PAGE_TIMEOUT + SINGLE_JS_TIMEOUT + BG_JS_TIMEOUT + 1000)
	public void checkTestWebsiteByWebclient(String websiteRelUrl) throws Exception {
		processWebsiteWithWebclient(TestHttpServer.absoluteUrl(websiteRelUrl));
	}

	protected void processWebsiteWithWebclient(String websiteUrl)
			throws Exception, ParameterException, ResourceException,
			StorageException {
		defineTestsExpectations();
		initJobContextAndParams(websiteUrl);

		// Process page.
		webClientTask.process();
		 jobContext.flush();

		// Asserts.
		Assert.assertTrue(follower.isSuccessfull(),"Task is not sucessful for page: " + websiteUrl);
	}		
	
	@Test(enabled = false, dataProvider = "websitesToTest", timeOut=DEFAULT_TEST_TIMEOUT)
	public void checkTestWebsiteByHtmlUnit(String websiteRelUrl) throws Exception {
		WebClient wc = new WebClient();
		wc.setTimeout(PAGE_TIMEOUT);
		wc.setJavaScriptEnabled(true);
		wc.setJavaScriptTimeout(SINGLE_JS_TIMEOUT);
		wc.setRedirectEnabled(false);
		wc.setActiveXNative(false);
		// fetch the page
		wc.getPage(TestHttpServer.absoluteUrl(websiteRelUrl));

		wc.waitForBackgroundJavaScript(BG_JS_TIMEOUT);
		wc.getJavaScriptEngine().shutdownJavaScriptExecutor();
				
		// no assert here: all tests should end without test timeout  
	}
	
	@DataProvider(name = "externalWebsitesToTest")
	public Object[][] getExternalWebsitesToTest() throws Exception {
		return new Object[][]{
				{"http://www.tvn24.pl/"},
				{"http://www.cnn.com/"},
				{"http://www.gazeta.pl"}
		};
	}
	
	// those tests are highly unstable (processing time varies from 2 to 20 seconds!)
	@Test(enabled=false, dataProvider="externalWebsitesToTest", timeOut=DEFAULT_TEST_TIMEOUT * 2)
	public void testWithExternalPage(String websiteUrl) throws Exception {
		processWebsiteWithWebclient(websiteUrl);
	}
	
	@Test(enabled=false)
	public void testFbCom() throws ParameterException, ResourceException, StorageException, Exception {
		processWebsiteWithWebclient("http://fb.com");
	}
}
