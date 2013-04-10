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

import org.testng.Assert;
import org.testng.annotations.Test;

public class ServiceDataTest {

	@Test
	public void breakingSpaceTest() {
		ServiceData serviceData = new ServiceData("http://test.test/test test", "");
		Assert.assertEquals(serviceData.getUrlForProcessing(), "http://test.test/test%20test");
	}

	@Test
	public void nonBreakingSpaceTest() {
		ServiceData serviceData = new ServiceData("http://test.test/test\u00A0test", "");
		Assert.assertEquals(serviceData.getUrlForProcessing(), "http://test.test/test%20test");
	}

	@Test
	public void mixedSpaceTest() {
		ServiceData serviceData = new ServiceData("http://test.test/test\u00A0test?id=43 26", "");
		Assert.assertEquals(serviceData.getUrlForProcessing(), "http://test.test/test%20test?id=43%2026");
	}
}
