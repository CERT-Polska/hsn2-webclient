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

package pl.nask.hsn2.service.urlfollower;

import java.net.URISyntaxException;

public final class FrameLink extends Link {
	private final WebClientOrigin origin;

	public static FrameLink getInstance(String baseUrl, String hrefAttrValue, WebClientOrigin originType) throws URISyntaxException {
		return new FrameLink(baseUrl, hrefAttrValue, originType);
	}

	private FrameLink(String baseUrl, String hrefAttrValue, WebClientOrigin originType) throws URISyntaxException {
		super(baseUrl, hrefAttrValue);
		this.origin = originType;
	}

	public String getOriginName() {
		return origin.getName();
	}

	@Override
	public String toString() {
		return "OutgoingLink [relativeUrl=" + getAbsoluteUrl() + ", originType" + origin.getName() + "]";
	}
}
