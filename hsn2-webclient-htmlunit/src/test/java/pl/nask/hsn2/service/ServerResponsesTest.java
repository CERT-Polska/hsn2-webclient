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

import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import pl.nask.hsn2.ParameterException;
import pl.nask.hsn2.ResourceException;
import pl.nask.hsn2.ServiceConnector;
import pl.nask.hsn2.StorageException;
import pl.nask.hsn2.server.TestHttpServer;
import pl.nask.hsn2.service.task.WebClientTaskContext;
import pl.nask.hsn2.service.urlfollower.HtmlUnitFollower;
import pl.nask.hsn2.utils.DataStoreHelper;

public class ServerResponsesTest {
	@Mocked
	ServiceConnector connector;

	@BeforeClass
	public void initServer() throws Exception {
		TestHttpServer.startServer();
	}

	@AfterClass
	public void stopServer() throws Exception {
		TestHttpServer.stopServer();
	}

	@Test
	public void urlWithoutSlashRedirectTest() throws Exception {
		HtmlUnitFollower f = new HtmlUnitFollower(TestHttpServer.getWebserverAddress(), new WebClientTaskContext(0, 0, 0, connector), new ServiceParameters());
		f.processUrl();

		Assert.assertNull(f.getProcessedPage().getClientSideRedirectPage());
	}

	private HtmlUnitFollower newFollower(String url) throws ParameterException, StorageException, ResourceException {
		new NonStrictExpectations() {
			@SuppressWarnings("unused")
			final DataStoreHelper forTestOnlyDataStoreHelper = null;
			{
				DataStoreHelper.saveInDataStore(connector, anyLong, withInstanceOf(InputStream.class));
				result = 1l;
				DataStoreHelper.saveInDataStore(connector, anyLong, withInstanceOf(byte[].class));
				result = 1l;
			}
		};

		WebClientTaskContext ctx = new WebClientTaskContext(0, 0, 0, connector);
		String absouteUrl = TestHttpServer.absoluteUrl(url);
		ctx.setServiceData(new ServiceData(absouteUrl, absouteUrl));
		ServiceParameters params = new ServiceParameters();
		ctx.setServiceParams(params);
		HtmlUnitFollower f = new HtmlUnitFollower(absouteUrl, ctx, params);
		ctx.webContextInit(absouteUrl, "http://referrer", 1L);
		return f;
	}

	@Test
	public void serverSideRedirectTest() throws Exception {
		HtmlUnitFollower f = newFollower("movedContext/aaa.html");
		f.processUrl();

		Assert.assertTrue(f.isSuccessfull(), "successful");
		Assert.assertEquals(f.getProcessedPage().getServerSideRedirectLocation(), TestHttpServer.absoluteUrl("newLocation/aaa.html"), "redirect location");
	}

	@Test
	public void serverSideRedirectToAbsoluteUrlTest() throws Exception {
		HtmlUnitFollower f = newFollower("serverSideRedirectTest.ssrAbsolute");
		f.processUrl();

		Assert.assertTrue(f.isSuccessfull(), "successful");
		Assert.assertEquals(f.getProcessedPage().getServerSideRedirectLocation(), TestHttpServer.absoluteUrl("realpage.html"), "redirect location");
	}

	@Test
	public void serverSideRedirectRelativeToSeverRootTest() throws Exception {
		HtmlUnitFollower f = newFollower("serverSideRedirectTest/some/other/directories/ssrRelativeToSeverRoot");
		f.processUrl();

		Assert.assertTrue(f.isSuccessfull(), "successful");
		Assert.assertEquals(f.getProcessedPage().getServerSideRedirectLocation(), TestHttpServer.absoluteUrl("realpage.html"), "redirect location");
	}

	@Test
	public void serverSideRedirectRelativeToDirectoryTest() throws Exception {
		HtmlUnitFollower f = newFollower("serverSideRedirectTest/ssrRelativeToDirectory");
		f.processUrl();

		Assert.assertTrue(f.isSuccessfull(), "successful");
		Assert.assertEquals(f.getProcessedPage().getServerSideRedirectLocation(), TestHttpServer.absoluteUrl("serverSideRedirectTest/realpage.html"),
				"redirect location");
	}

	@Test
	public void test404() throws Exception {
		HtmlUnitFollower f = newFollower("nonexistentResource");
		f.processUrl();

		Assert.assertTrue(f.isSuccessfull(), "follower.successfull");
		Assert.assertNull(f.getFailureMessage(), "follower.failureMessage");
		Assert.assertEquals(f.getProcessedPage().getResponseCode(), 404, "response code");
	}

	@Test
	public void swfTest() throws Exception {
		HtmlUnitFollower follower = newFollower("files/swfFile.swf");
		follower.processUrl();

		Assert.assertTrue(follower.isSuccessfull());
		Assert.assertEquals(follower.getContentType(), "application/x-shockwave-flash", "flash content type");
	}

	@Test
	public void serverSideRedirectToEmptyLocation() throws Exception {
		HtmlUnitFollower f = newFollower("serverSideRedirectTest/ssrEmptyLocation");
		f.processUrl();
		Assert.assertNull(f.getFailureMessage(), "Failure message should be null.");
		Assert.assertNull(f.getProcessedPage().getServerSideRedirectLocation(), "Server side redirect location should be null.");
	}

	@Test
	public void serverSideRedirectNoLocation() throws Exception {
		HtmlUnitFollower f = newFollower("serverSideRedirectTest/ssrNoLocation");
		f.processUrl();
		Assert.assertNull(f.getFailureMessage(), "Failure message should be null.");
		Assert.assertNull(f.getProcessedPage().getServerSideRedirectLocation(), "Server side redirect location should be null.");
	}
}
