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

import java.net.URISyntaxException;

import org.testng.Assert;
import org.testng.annotations.Test;

import pl.nask.hsn2.service.urlfollower.Link;

@Test
public class LinkTest {

	public void testCommonRelativeUrl() throws URISyntaxException {
		Link link = new Link("http://nask.pl/aaa.gif", "index.html");
		Assert.assertEquals(link.getBaseUrl(), "http://nask.pl/aaa.gif");
		Assert.assertEquals(link.getRelativeUrl(), "index.html");
		Assert.assertEquals(link.getAbsoluteUrl(), "http://nask.pl/index.html");
	}
	
	public void testRelativeUrlIsAbsolute() throws URISyntaxException {
		Link link = new Link("http://task.pl/aaa.gif", "http://nask.pl/index.html");
		Assert.assertEquals(link.getAbsoluteUrl(), "http://nask.pl/index.html");
	}
	
	public void testRelativeUrlWithWhitespaces() throws URISyntaxException {
		Link link = new Link("http://nask.pl/aaa.gif", "http://nask.pl/i n d e x.html");
		Assert.assertEquals(link.getAbsoluteUrl(), "http://nask.pl/i%20n%20d%20e%20x.html");
	}
	
	public void testRelativeUrlWIthSpacesInQueryPart() throws URISyntaxException {
		Link link = new Link("http://nask.pl/aaa.gif", "index.php?a=b c d");
		Assert.assertEquals(link.getAbsoluteUrl(), "http://nask.pl/index.php?a=b%20c%20d");	
	}

	public void testBaseUrlWithSpace() throws URISyntaxException {
		Link link = new Link("http://nask.pl/aa a.gif", "index.php?a=b c d");
		Assert.assertEquals(link.getAbsoluteUrl(), "http://nask.pl/index.php?a=b%20c%20d");	
	}
	public void testISSencoding() throws URISyntaxException {
		Link link = new Link("http://nask.pl/aa a.gif","ind%u0065x.php?a=b c d");
		Assert.assertEquals(link.getAbsoluteUrl(), "http://nask.pl/ind%u0065x.php?a=b%20c%20d");	
	}
	
	public void testISSencoding2() throws URISyntaxException {
		Link link = new Link("http://nask.pl/aa a.gif","%u0065x.php?a=b c d");
		Assert.assertEquals(link.getAbsoluteUrl(), "http://nask.pl/%u0065x.php?a=b%20c%20d");	
	}
	
	public void testRelativeUrlIsAbsoluteIIS() throws URISyntaxException {
		Link link = new Link("http://task.pl/aaa.gif", "http://nask.pl/ind%u0065x.html");
		Assert.assertEquals(link.getAbsoluteUrl(), "http://nask.pl/ind%u0065x.html");
	}
	
	public void testIISdecoding() throws URISyntaxException {
		Link link = new Link("http://task.pl/aaa.gif", "http://nask.pl/ind%u0065x.html",true);
		Assert.assertEquals(link.getAbsoluteUrl(), "http://nask.pl/index.html");
	}
	public void testUTF16toUTF8conversion() throws URISyntaxException {
		Link link = new Link("http://task.pl/aaa.gif", "http://nask.pl/ind%u0142x.html",true);
		Assert.assertEquals(link.getAbsoluteUrl(), "http://nask.pl/ind%C5%82x.html");
	}
	
}
