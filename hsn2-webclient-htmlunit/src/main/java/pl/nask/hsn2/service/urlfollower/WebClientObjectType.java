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

package pl.nask.hsn2.service.urlfollower;

import pl.nask.hsn2.service.ServiceParameters;

public enum WebClientObjectType {
	// SWF("swf", "application/x-shockwave-flash"),
	// PDF("pdf", "application/pdf"),
	URL("url", null) {
		@Override
		public boolean isElliglibeForExtract(ServiceParameters params) {
			return false;
		}
	},
	FILE("file", null) {
		@Override
		public boolean isElliglibeForExtract(ServiceParameters params) {
			//return params.isSaveObjects();
			return true;
		}
	};

	public static WebClientObjectType forMimeType(String mimeType) {
		for (WebClientObjectType type : values()) {
			if (type.getMimeType() != null && type.getMimeType().equals(mimeType)) {
				return type;
			}
		}
		return FILE;
	}

	private String name;
	private String mimeType;

	WebClientObjectType(String name, String mimeType) {
		this.name = name;
		this.mimeType = mimeType;
	}

	public String getName() {
		return name;
	}

	public String getMimeType() {
		return mimeType;
	}

	public abstract boolean isElliglibeForExtract(ServiceParameters params);
}
