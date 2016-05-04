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

import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.Assert;
import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import pl.nask.hsn2.ServiceConnector;
import pl.nask.hsn2.bus.operations.Attribute;
import pl.nask.hsn2.bus.operations.ObjectData;
import pl.nask.hsn2.bus.operations.ObjectResponse;
import pl.nask.hsn2.connector.REST.DataResponse;
import pl.nask.hsn2.server.TestHttpServer;
import pl.nask.hsn2.service.task.WebClientTaskContext;
import pl.nask.hsn2.service.urlfollower.HtmlUnitFollower;
import pl.nask.hsn2.utils.WebClientDataStoreHelper;
import pl.nask.hsn2.wrappers.CookieWrapper;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class PageAndJavascriptInteractionTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(PageAndJavascriptInteractionTest.class);
	public static final String REFERRER = "http://referrer/referrer.html";
	private static final Set<CookieWrapper> COOKIE_WRAPPERS = new HashSet<CookieWrapper>();
	
	@Mocked
	private ServiceConnector connector;
	
	private ServiceParameters params;
	private HtmlUnitFollower follower;
	private WebClientTask webClientTask;
	private WebClientTaskContext jobContext;
	private Set<String> links;

	@BeforeClass
	public void initServer() throws Exception {
		TestHttpServer.startServer("pages");
	}

	@AfterClass
	public void stopServer() throws Exception {
		TestHttpServer.stopServer();
	}

	private void checkForAttributes(List<ObjectData> dataList) {
		for (ObjectData objectData : dataList) {
			LOGGER.info("Saving objects:");
			Set<Attribute> attrs = objectData.getAttributes();
			Set<String> attrs2 = new TreeSet<String>();
			for (Attribute attribute : attrs) {
				attrs2.add(attribute.getName() + "=" + attribute.getValue());
				if (attribute.getName() == "url_original") {
					links.add(attribute.getValue().toString());
				}
			}
			for (String attribute : attrs2) {
				LOGGER.info("###--> " + attribute);
			}
		}
	}

	private void defineConnectorExpectations() throws Exception {
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
			}
		};
	}

	private void defineWebClientDataStoreHelperExpectations() throws Exception {
		new NonStrictExpectations() {
			@SuppressWarnings("unused")
			WebClientDataStoreHelper helper = null;
			@SuppressWarnings("unused")
			final WebClientDataStoreHelper wcHelper = null;
			{
				WebClientDataStoreHelper.getCookiesFromDataStore(connector, anyLong, anyLong);
				result = COOKIE_WRAPPERS;
			}
		};
	}

	private void initJobContextAndParams(String testPageAbsoluteUrl) throws Exception {
		links = new HashSet<String>();
		jobContext = new WebClientTaskContext(1, 1, 1, connector);
		params = new ServiceParameters();
		params.setSaveHtml(false);
		params.setSaveCookies(false);
		ServiceData serviceData = new ServiceData(1l, testPageAbsoluteUrl, testPageAbsoluteUrl, REFERRER, 1l, 5, 1l, null);
		follower = new HtmlUnitFollower(testPageAbsoluteUrl, jobContext, params);
		webClientTask = new WebClientTask(jobContext, params, serviceData, follower);
	}

	/**
	 * Test to confirm that HtmlUnit's Page is dynamically changing all the time
	 * and JS scripts can't create new content even after Page has been got from
	 * web client.
	 * 
	 * @throws Exception
	 */
	@Test
	public void gargoyleSoftHtmlUnitWebClientTest1() throws Exception {
		WebClient wc = new WebClient();
		wc.setJavaScriptTimeout(1000);
		HtmlPage p = wc.getPage(TestHttpServer.absoluteUrl("pageAndJavascriptInteraction.html"));
		wc.waitForBackgroundJavaScript(500);
		// No JS shutting down here.
		DomElement element = p.getElementById("el2");
		String eTextBefore = element.getTextContent();
		Thread.sleep(2500);
		String eTextAfter = element.getTextContent();
		Assert.assertEquals(eTextBefore, "...");
		// JS engine has not been shut down, and therefore text has been changed by background script.
		Assert.assertEquals(eTextAfter, "link2");
	}

	/**
	 * Test to confirm that HtmlUnit's Page can't be changed while JS engine has
	 * been shut down. Please note that one can't be sure that engine has been
	 * actually closed. All we can do is to try to shut it down (which usually
	 * happens quite fast).
	 * 
	 * @throws Exception
	 */
	@Test
	public void gargoyleSoftHtmlUnitWebClientTest2() throws Exception {
		WebClient wc = new WebClient();
		wc.setJavaScriptTimeout(1000);
		HtmlPage page = wc.getPage(TestHttpServer.absoluteUrl("pageAndJavascriptInteraction.html"));
		wc.waitForBackgroundJavaScript(500);

		// JS engine shut down initializing.
		wc.getJavaScriptEngine().shutdownJavaScriptExecutor();

		DomElement element = page.getElementById("el2");
		String eTextBefore = element.getTextContent();
		Thread.sleep(2500);
		String eTextAfter = element.getTextContent();
		Assert.assertEquals(eTextBefore, "...");
		// If JS engine has been shut down, both Strings should be the same.
		Assert.assertEquals(eTextAfter, "...");
	}

	/**
	 * HSN2 WebClient test with short timeout for background scripts.
	 * It should cut off 2nd script and therefore it should not change
	 * page content. 
	 * 
	 * @throws Exception
	 */
	@Test
	public void webClientTestShortBkgrTimeout() throws Exception {
		defineConnectorExpectations();
		defineWebClientDataStoreHelperExpectations();
		initJobContextAndParams(TestHttpServer.absoluteUrl("pageAndJavascriptInteraction.html"));
		params.setSaveCookies(false);
		params.setSingleJsTimeoutMillis(1000);
		params.setBackgroundJsTimeoutMillis(500);
		// Wait 2500 ms more to make sure.
		Thread.sleep(2500);
		webClientTask.process();
		jobContext.flush();
		// First script runs fine so a1 link should be reported.
		Assert.assertTrue(links.contains(TestHttpServer.absoluteUrl("a1.html")));
		
		// Second script should be cut off, a2 link should not be reported.
		Assert.assertFalse(links.contains(TestHttpServer.absoluteUrl("a2.html")));
	}

	/**
	 * HSN2 WebClient test with long timeout for background scripts.
	 * Both script's changes should be reported.
	 * 
	 * @throws Exception
	 */
	@Test
	public void webClientTestShortLongTimeout() throws Exception {
		defineConnectorExpectations();
		defineWebClientDataStoreHelperExpectations();
		initJobContextAndParams(TestHttpServer.absoluteUrl("pageAndJavascriptInteraction.html"));
		params.setSaveCookies(false);
		params.setSingleJsTimeoutMillis(1000);
		params.setBackgroundJsTimeoutMillis(2500);
		// Thread sleep not needed here.
		webClientTask.process();
		jobContext.flush();
		// Both scripts run fine so a1 and a2 link should be reported.
		Assert.assertTrue(links.contains(TestHttpServer.absoluteUrl("a1.html")));
		Assert.assertTrue(links.contains(TestHttpServer.absoluteUrl("a2.html")));
	}
}
