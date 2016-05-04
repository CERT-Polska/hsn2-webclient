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

import org.testng.Assert;
import org.testng.annotations.Test;

import pl.nask.hsn2.bus.operations.Attribute;
import pl.nask.hsn2.bus.operations.AttributeType;
import pl.nask.hsn2.bus.operations.ObjectData;
import pl.nask.hsn2.service.task.NewWebClientUrlObject;

public class NewWebClientUrlObjectTest {
	private NewWebClientUrlObject newWebClientUrlObject;
	private ObjectData dataObject;
	private static final String URL = "http://url.for.test/jd7ey48s";
	private static final String REFERRER = "http://referrer.for.test/h4a30ca";
	private static final String ORIGINAL = "origin-n59xhd";
	private static final String TYPE = "type-m19f54a";
	private static final long PARENT_ID = 123L;
	private static final long JOB_ID = 123L;

	@Test
	public void serializationDeserializationTest() throws Exception {
		newWebClientUrlObject = new NewWebClientUrlObject(URL, ORIGINAL, TYPE, REFERRER, JOB_ID);
		dataObject = newWebClientUrlObject.asDataObject(PARENT_ID);
		Attribute tempAttr;

		tempAttr = dataObject.findAttribute("referrer", AttributeType.STRING);
		Assert.assertNotNull(tempAttr, "Can't find referrer attribute");
		Assert.assertEquals(tempAttr.getString(), REFERRER, "Attribute referrer has wrong value");

		tempAttr = dataObject.findAttribute("type", AttributeType.STRING);
		Assert.assertNotNull(tempAttr, "Can't find type attribute");
		Assert.assertEquals(tempAttr.getString(), TYPE, "Attribute type has wrong value");

		tempAttr = dataObject.findAttribute("origin", AttributeType.STRING);
		Assert.assertNotNull(tempAttr, "Can't find origin attribute");
		Assert.assertEquals(tempAttr.getString(), ORIGINAL, "Attribute origin has wrong value");

		tempAttr = dataObject.findAttribute("url_original", AttributeType.STRING);
		Assert.assertNotNull(tempAttr, "Can't find url_original attribute");
		Assert.assertEquals(tempAttr.getString(), URL, "Attribute url_original has wrong value");

		tempAttr = dataObject.findAttribute("parent", AttributeType.OBJECT);
		Assert.assertNotNull(tempAttr, "Can't find parent attribute");
		Assert.assertEquals(tempAttr.getObejectRef(), PARENT_ID, "Attribute parent has wrong value");

		Assert.assertNotNull(dataObject.findAttribute("referrer_cookie", AttributeType.BYTES), "Can't find referrer_cookie attribute");
		Assert.assertNotNull(dataObject.findAttribute("creation_time", AttributeType.TIME), "Can't find time attribute");
	}

	@Test
	public void parentNullTest() throws Exception {
		newWebClientUrlObject = new NewWebClientUrlObject(URL, ORIGINAL, TYPE, REFERRER, JOB_ID);
		dataObject = newWebClientUrlObject.asDataObject(null);
		Attribute tempAttr = dataObject.findAttribute("parent", AttributeType.OBJECT);
		Assert.assertNull(tempAttr, "ParentId attribute should be null.");
	}
	@Test
	public void referrerNullTest() throws Exception {
		newWebClientUrlObject = new NewWebClientUrlObject(URL, ORIGINAL, TYPE, null, JOB_ID);
		dataObject = newWebClientUrlObject.asDataObject(null);
		Attribute tempAttr = dataObject.findAttribute("referrer", AttributeType.OBJECT);
		Assert.assertNull(tempAttr, "Referrer attribute should be null.");
	}
}
