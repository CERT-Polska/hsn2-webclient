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
import java.util.Map;
import java.util.Set;

import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import pl.nask.hsn2.ServiceConnector;
import pl.nask.hsn2.bus.operations.AttributeType;
import pl.nask.hsn2.server.TestHttpServer;
import pl.nask.hsn2.service.task.WebClientTaskContext;
import pl.nask.hsn2.service.urlfollower.EmbeddedResource;
import pl.nask.hsn2.service.urlfollower.HtmlUnitFollower;
import pl.nask.hsn2.service.urlfollower.OutgoingLink;
import pl.nask.hsn2.service.urlfollower.PageLinks.LinkType;
import pl.nask.hsn2.utils.DataStoreHelper;

public class NaskPlTest {
	private final int timeout = 2000;
	private HtmlUnitFollower follower;
	WebClientTaskContext ctx;
	String url;
	@Mocked
	ServiceConnector connector;

	@BeforeClass
	public void initServer() throws Exception {
		TestHttpServer.startServer("naskPl");
	}

	@AfterClass
	public void stopServer() throws Exception {
		TestHttpServer.stopServer();
	}

	@BeforeMethod
	public void beforeMethod() throws Exception {
		new NonStrictExpectations() {
			@SuppressWarnings("unused")
			final DataStoreHelper forTestOnlyDataStoreHelper = null;
			{
				pl.nask.hsn2.utils.DataStoreHelper.saveInDataStore(connector, anyLong, withInstanceOf(InputStream.class));
				result = 1l;
			}
		};
	}

	private void processHandledSimpleUrl() throws Exception {
		ServiceParameters params = new ServiceParameters();
		params.setSaveImages(true);
		params.setSaveObjects(true);
		params.setSaveOthers(true);
		ctx = new WebClientTaskContext(0, 0, 0, null);
		url = TestHttpServer.absoluteUrl("/");
		ServiceData serviceData = new ServiceData(url, url);
		follower = new HtmlUnitFollower(TestHttpServer.getWebserverRoot(), ctx, params);
		WebClientTask webClientTask = new WebClientTask(ctx, params, serviceData, follower);
		webClientTask.process();
	}

	@Test(timeOut = timeout)
	public void testNaskPl() throws Exception {
		processHandledSimpleUrl();
		Assert.assertNotNull(follower, "follower");
		Assert.assertNotNull(follower.getPageLinks(), "follower.pageLinks");
	}

	@Test(dependsOnMethods = "testNaskPl")
	public void activeTest() {
		Assert.assertTrue(follower.isSuccessfull(), "isSuccessfull");
		Assert.assertNull(follower.getFailureMessage(), "failure message should be null");
		Assert.assertEquals(follower.getProcessedPage().getResponseCode(), 200, "response code");
	}

	@Test(dependsOnMethods = "testNaskPl")
	public void isHtmlTest() throws IOException {
		Assert.assertTrue(follower.getProcessedPage().isHtml(), "isHtml");
		Assert.assertNotNull(ctx.getContextByUrl(url).getObjectDataBuilder().build().findAttribute("html_source", AttributeType.BYTES),
				"no html_source ref attribute");

	}

	@Test(dependsOnMethods = "testNaskPl")
	public void testOutgoingLinks() {
		Set<OutgoingLink> list = follower.getPageLinks().getOutgoingLinks();
		Assert.assertEquals(list.size(), 15);
	}

	/**
	 * Test has been disabled. It checks deprecated method of gathering embedded
	 * files. At the moment all files are stored not as list in PageLinks but as
	 * separate objects in Object Store and there are other tests for this new
	 * functionality. (See test class EmbeddedResourcesTest.)
	 */
	@Test(dependsOnMethods = "testNaskPl", enabled = false)
	public void testEmbeddedFiles() {
		Map<LinkType, Set<EmbeddedResource>> map = follower.getPageLinks().getEmbeddedResourcesGroups();
		assertContains(map.get(LinkType.IMAGE), "about_nask01.gif", LinkType.IMAGE);
		assertContains(map.get(LinkType.OBJECT), "arakis-banner.swf", LinkType.OBJECT);
		assertContains(map.get(LinkType.IMAGE), "big_arrow.gif", LinkType.IMAGE);
		assertContains(map.get(LinkType.IMAGE), "bip-logo.gif", LinkType.IMAGE);
		assertContains(map.get(LinkType.IMAGE), "dealer_coop01.gif", LinkType.IMAGE);
		assertContains(map.get(LinkType.OTHER), "general.css", LinkType.OTHER);
		assertContains(map.get(LinkType.IMAGE), "grey-spacer.gif", LinkType.IMAGE);
		assertContains(map.get(LinkType.OBJECT), "loga-v2.swf", LinkType.OBJECT);
		assertContains(map.get(LinkType.IMAGE), "logo-nask_20_lat.gif", LinkType.IMAGE);
		assertContains(map.get(LinkType.OBJECT), "main-header.swf", LinkType.OBJECT);
		assertContains(map.get(LinkType.IMAGE), "menu_contact.gif", LinkType.IMAGE);
		assertContains(map.get(LinkType.IMAGE), "menu_english.gif", LinkType.IMAGE);
		assertContains(map.get(LinkType.IMAGE), "menu_sitemap.gif", LinkType.IMAGE);
		assertContains(map.get(LinkType.IMAGE), "menu-spacer.gif", LinkType.IMAGE);
		assertContains(map.get(LinkType.IMAGE), "news01.gif", LinkType.IMAGE);
		assertContains(map.get(LinkType.IMAGE), "news_text.gif", LinkType.IMAGE);
		assertContains(map.get(LinkType.IMAGE), "offer01.gif", LinkType.IMAGE);
		assertContains(map.get(LinkType.IMAGE), "oper_pers_center01.gif", LinkType.IMAGE);
		assertContains(map.get(LinkType.IMAGE), "praca01.gif", LinkType.IMAGE);
		assertContains(map.get(LinkType.IMAGE), "right_linesx.gif", LinkType.IMAGE);
		assertContains(map.get(LinkType.IMAGE), "search_button.gif", LinkType.IMAGE);
		assertContains(map.get(LinkType.IMAGE), "spacer.gif", LinkType.IMAGE);
		assertContains(map.get(LinkType.IMAGE), "spx.gif", LinkType.IMAGE);
		assertContains(map.get(LinkType.IMAGE), "teraz-polska-nask.png", LinkType.IMAGE);
		assertContains(map.get(LinkType.IMAGE), "under_flash_line.gif", LinkType.IMAGE);
		assertContains(map.get(LinkType.IMAGE), "under_table_line_spacer.gif", LinkType.IMAGE);
		assertContains(map.get(LinkType.IMAGE), "favicon.ico", LinkType.IMAGE);
		Assert.assertEquals(map.get(LinkType.IMAGE).size() + map.get(LinkType.OBJECT).size() + map.get(LinkType.OTHER).size(), 27, map.toString());
	}

	private void assertContains(Set<EmbeddedResource> list, String query, LinkType type) {
		EmbeddedResource foundRes = null;
		for (EmbeddedResource res : list) {
			String[] split = res.getAbsoluteUrl().split("/");

			if (split[split.length - 1].equals(query)) {
				foundRes = res;
				break;
			}
		}
		if (foundRes == null) {
			Assert.fail("Couldn't find resource " + query + " in " + list);
		} else {
			Assert.assertEquals(type, foundRes.getLinkType(), "Improper LinkType for resource " + foundRes);
		}
	}
}
