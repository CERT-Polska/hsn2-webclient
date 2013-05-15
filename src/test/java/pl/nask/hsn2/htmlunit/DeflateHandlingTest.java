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

package pl.nask.hsn2.htmlunit;

import java.io.IOException;
import java.net.MalformedURLException;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.JavaScriptPage;
import com.gargoylesoftware.htmlunit.ScriptException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.util.UrlUtils;

public class DeflateHandlingTest {
	/**
	 * This is a test case for a bug reported with issue#8269 - Seems like HtmlUnit has problems with deflated content.
	 * (This tests only the behaviour of HtmlUnit, not the service)
	 * 
	 * Test has been disabled, but has not been removed in case anyone would like to see if future versions of HtmlUnit
	 * solved the issue.
	 * 
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws FailingHttpStatusCodeException
	 */
	@Test(enabled = false, expectedExceptions = ScriptException.class, expectedExceptionsMessageRegExp = "ReferenceError: \"localStorage\" is not defined.*")
	public void testTvn24() throws FailingHttpStatusCodeException, MalformedURLException, IOException {		
		WebClient wc = prepareWc();
		WebRequest wr = new WebRequest(UrlUtils.toUrlUnsafe("http://www.tvn24.pl"));
		// removing "Accept-Encoding" does not solve the problem since HtmlUnit will restore this header from the referers (this) request
		// passing an empty value works fine
		// passing a 'gzip' also causes an exception here.
		wr.setAdditionalHeader("Accept-Encoding", "");		
		
		// javascript processing exception will be thrown here. If something changes, than the error with 'localStorage' was solved in the HtmlUnit 
		wc.getPage(wr);
	}

	/**
	 * The same test as testTvn24 - this time with the content which causes the error
	 * @throws FailingHttpStatusCodeException
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	@Test
	public void testGemiusScript() throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		WebClient wc =  new WebClient();		
		WebRequest wr = new WebRequest(UrlUtils.toUrlUnsafe("http://pro.hit.gemius.pl/hmapxy.js"));
		// remove this line to check, if 'deflate' handling started to work fine in this case
		wr.setAdditionalHeader("Accept-Encoding", "");
		JavaScriptPage p = wc.getPage(wr);		
		Assert.assertNotNull(p);
		Assert.assertNotNull(p.getContent());
	}		

	private WebClient prepareWc() {
		WebClient wc = new WebClient();
		wc.getOptions().setJavaScriptEnabled(true);
		return wc;
	}
}
