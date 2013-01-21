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

import mockit.Mocked;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import pl.nask.hsn2.ServiceConnector;
import pl.nask.hsn2.server.TestHttpServer;
import pl.nask.hsn2.service.task.WebClientTaskContext;
import pl.nask.hsn2.service.urlfollower.HtmlUnitFollower;

public class JsRedirectTest {
	private static final Logger LOGGER = LoggerFactory.getLogger("JsRedirectTest");
	public static final String REFERRER = "http://referrer/referrer.html";
	@Mocked
	private ServiceConnector connector;
	private ServiceParameters params;
	private HtmlUnitFollower follower;
	private WebClientTask webClientTask;
	private WebClientTaskContext jobContext;
	
	@BeforeClass
	public void initServer() throws Exception {
		TestHttpServer.startServer("jsRedirect");
	}
	
	
	@Test
    public void jsRedirectSelfLocationTwoTimesTest() throws Exception {
		String testPageAbsoluteUrl = TestHttpServer.absoluteUrl("jsRedirectSelfLocationToAnother.html");
		initJobContextAndParams(testPageAbsoluteUrl);
		webClientTask.process();
		Assert.assertEquals(follower.getProcessedPage().getActualUrl().toExternalForm(), testPageAbsoluteUrl);
		Assert.assertEquals(follower.getProcessedPage().getClientSideRedirectPage().getActualUrl().toExternalForm(), TestHttpServer.absoluteUrl("jsRedirectSelfLocationToEnd.html"));
		Assert.assertEquals(follower.getProcessedPage().getLastPage().getActualUrl().toExternalForm(), TestHttpServer.absoluteUrl("end.html"));
    }

	@Test
    public void jsRedirectSelfLocationToSiteWithJsFileTest() throws Exception {
		String testPageAbsoluteUrl = TestHttpServer.absoluteUrl("jsRedirectSelfLocationToSiteWithJsFile.html");
		initJobContextAndParams(testPageAbsoluteUrl);
		webClientTask.process();
	
		Assert.assertEquals(follower.getProcessedPage().getActualUrl().toExternalForm(), testPageAbsoluteUrl);
		Assert.assertEquals(follower.getProcessedPage().getLastPage().getActualUrl().toExternalForm(), TestHttpServer.absoluteUrl("endWithJsFileFiredOnLoadBody.html"));
		Assert.assertNull(follower.getProcessedPage().getClientSideRedirectPage().getClientSideRedirectPage());
	}
	
	@Test
    public void jsRedirectSelfLocationInOnLoadFromJsTest() throws Exception {
		String testPageAbsoluteUrl = TestHttpServer.absoluteUrl("jsRedirectSelfLocationToOnLoadFromJs1.html");
		initJobContextAndParams(testPageAbsoluteUrl);
		webClientTask.process();
		
		Assert.assertEquals(follower.getProcessedPage().getActualUrl().toExternalForm(), testPageAbsoluteUrl);
		Assert.assertEquals(follower.getProcessedPage().getClientSideRedirectPage().getActualUrl().toExternalForm(), TestHttpServer.absoluteUrl("jsRedirectSelfLocationToOnLoadFromJs2.html"));
		Assert.assertEquals(follower.getProcessedPage().getClientSideRedirectPage().getClientSideRedirectPage().getActualUrl().toExternalForm(), TestHttpServer.absoluteUrl("jsRedirectSelfLocationInOnLoadFromJs.html"));
		Assert.assertEquals(follower.getProcessedPage().getLastPage().getActualUrl().toExternalForm(), TestHttpServer.absoluteUrl("end.html"));
	}
	
	@Test
    public void serverRedirectWithJsRedirectTest() throws Exception {
		String testPageAbsoluteUrl = TestHttpServer.absoluteUrl("movedContext/serverRedirectWithJsRedirect.html");
		initJobContextAndParams(testPageAbsoluteUrl);
		webClientTask.process();
	
		Assert.assertEquals(follower.getProcessedPage().getActualUrl().toExternalForm(), testPageAbsoluteUrl);
		Assert.assertEquals(follower.getProcessedPage().getServerSideRedirectLocation(), TestHttpServer.absoluteUrl("newLocation/serverRedirectWithJsRedirect.html"));
		Assert.assertNotNull(jobContext.getContextByUrl(TestHttpServer.absoluteUrl("end.html")));
	}
	
	@Test
    public void jsRedirectToServerRedirectWithJsRedirectTest() throws Exception {
		String testPageAbsoluteUrl = TestHttpServer.absoluteUrl("jsRedirectToServerRedirectWithJsRedirect.html");
		initJobContextAndParams(testPageAbsoluteUrl);
		webClientTask.process();
	
		Assert.assertEquals(follower.getProcessedPage().getActualUrl().toExternalForm(), testPageAbsoluteUrl);
		Assert.assertEquals(follower.getProcessedPage().getClientSideRedirectPage().getActualUrl().toExternalForm(), TestHttpServer.absoluteUrl("movedContext/serverRedirectWithJsRedirect.html"));
		Assert.assertEquals(follower.getProcessedPage().getClientSideRedirectPage().getServerSideRedirectLocation(), TestHttpServer.absoluteUrl("newLocation/serverRedirectWithJsRedirect.html"));
		Assert.assertNotNull(jobContext.getContextByUrl(TestHttpServer.absoluteUrl("end.html")));
	}
	
	@Test
    public void twoTimesJsRedirectToServerRedirectTest() throws Exception {
		String testPageAbsoluteUrl = TestHttpServer.absoluteUrl("jsRedirectToJsRedirectWithServerRedirect.html");
		initJobContextAndParams(testPageAbsoluteUrl);
		webClientTask.process();
	
		Assert.assertEquals(follower.getProcessedPage().getActualUrl().toExternalForm(), testPageAbsoluteUrl);
		Assert.assertEquals(follower.getProcessedPage().getClientSideRedirectPage().getActualUrl().toExternalForm(), TestHttpServer.absoluteUrl("jsRedirectToServerRedirect.html"));
		Assert.assertEquals(follower.getProcessedPage().getClientSideRedirectPage().getClientSideRedirectPage().getActualUrl().toExternalForm(), TestHttpServer.absoluteUrl("movedContext/end.html"));
		Assert.assertNotNull(jobContext.getContextByUrl(TestHttpServer.absoluteUrl("newLocation/end.html")));
	}
	
	private void initJobContextAndParams(String testPageAbsoluteUrl) throws Exception {
		
		jobContext = new WebClientTaskContext(1, 1, 1, connector);
		params = new ServiceParameters();
		ServiceData serviceData = new ServiceData(1l, testPageAbsoluteUrl, testPageAbsoluteUrl, REFERRER, null, 5, null);
		follower = new HtmlUnitFollower(testPageAbsoluteUrl, jobContext, params);
		webClientTask = new WebClientTask(jobContext, params, serviceData, follower);
	}
	
	
}
