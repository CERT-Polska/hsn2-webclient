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

import junit.framework.Assert;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import pl.nask.hsn2.ServiceConnector;
import pl.nask.hsn2.connector.REST.DataResponse;
import pl.nask.hsn2.server.TestHttpServer;
import pl.nask.hsn2.service.task.WebClientTaskContext;
import pl.nask.hsn2.service.urlfollower.HtmlUnitFollower;
import pl.nask.hsn2.service.urlfollower.JsScriptErrorListener;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.JavaScriptEngine;

public class WebClientTimeoutsTest {
	public static final String WEBPAGE_ROOT = "pages";

	private HtmlUnitFollower follower;
	private ServiceParameters params;
	private WebClientTask webClientTask;
	private WebClientTaskContext taskContext;

	@Mocked
	JsScriptErrorListener listener;

	@Mocked
	ServiceConnector serviceConnector;

	private void prepareTests(String absPageUrl) throws Exception {
		params = new ServiceParameters();
		params.setSaveHtml(false);
		params.setSaveCookies(false);
		params.setProcessExternalLinks(false);
		taskContext = new WebClientTaskContext(0, 0, 0, serviceConnector);
		follower = new HtmlUnitFollower(absPageUrl, taskContext, params);
		webClientTask = new WebClientTask(taskContext, params, new ServiceData(absPageUrl, ""), follower);

		new NonStrictExpectations() {
			{
				byte[] anyData = null;
				serviceConnector.sendDataStoreData(anyLong, anyData);
				result = new DataResponse(1L);
			}
		};

		// check if shutdownJavaScriptExecutor is invoked every in each case
		new MockUp<JavaScriptEngine>() {
			@Mock(minInvocations = 1)
			public void shutdownJavaScriptExecutor() {
			}
		};
	}

	@BeforeClass
	public void startServer() throws Exception {
		TestHttpServer.startServer(WEBPAGE_ROOT);
	}

	@AfterClass
	public void stopServer() throws Exception {
		TestHttpServer.stopServer();
	}

	@Test(timeOut = 2000)
	public void singleJsTimeout() throws Exception {
		final int singleJsTimeout = 100;
		new NonStrictExpectations() {
			{
				listener.timeoutError(withInstanceOf(HtmlPage.class), singleJsTimeout, anyLong);
				times = 1;
				forEachInvocation = new Object() {
					@SuppressWarnings("unused")
					void validate(HtmlPage hp, long allowedTime, long executionTime) {
						Assert.assertEquals((executionTime >= singleJsTimeout), true);
					}
				};
			}
		};

		prepareTests(TestHttpServer.absoluteUrl("jsLongSingleScriptTimeout.html"));
		params.setPageTimeoutMillis(999999);
		params.setSingleJsTimeoutMillis(singleJsTimeout);
		params.setBackgroundJsTimeoutMillis(999999);
		webClientTask.process();
	}

	@Test(timeOut = 2000)
	public void backgroundJsTimeout() throws Exception {
		prepareTests(TestHttpServer.absoluteUrl("jsLongBackgroundScriptTimeout.html"));
		params.setPageTimeoutMillis(999999);
		params.setSingleJsTimeoutMillis(999999);
		params.setBackgroundJsTimeoutMillis(100);
		webClientTask.process();

	}

	@Test(timeOut = 2000)
	public void pageTimeout() throws Exception {
		prepareTests(TestHttpServer.absoluteUrl("jsLongSingleScriptTimeout.html"));
		params.setPageTimeoutMillis(100);
		params.setSingleJsTimeoutMillis(999999);
		params.setBackgroundJsTimeoutMillis(999999);
		webClientTask.process();
		Assert.assertFalse(follower.isSuccessfull());
		Assert.assertTrue(follower.getFailureMessage().startsWith("Timeout"));
	}

	@Test
	public void processingTimeoutLTPageTimeout() throws Exception {
		prepareTests(TestHttpServer.absoluteUrl("jsTwoSecondsJavaScript.html"));
		params.setProcessingTimeout(500);
		params.setPageTimeoutMillis(1000);
		params.setSingleJsTimeoutMillis(999999);
		params.setBackgroundJsTimeoutMillis(999999);
		webClientTask.process();
		Assert.assertFalse(follower.isSuccessfull());
		Assert.assertTrue(follower.getFailureMessage().contains("Timeout"));
	}
}
