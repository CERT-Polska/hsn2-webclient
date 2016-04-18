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

package pl.nask.hsn2.service.task;

import org.apache.commons.httpclient.URIException;

import pl.nask.hsn2.NewUrlObject;
import pl.nask.hsn2.bus.operations.ObjectData;
import pl.nask.hsn2.bus.operations.builder.ObjectDataBuilder;
import pl.nask.hsn2.utils.DataStoreHelper;

public class NewWebClientUrlObject extends NewUrlObject {

	private String referrer;
	private Long referrerCookieId;
	private Long contentId;
	private String mimeType;
	private Long downloadTimeStart;
	private Long downloadTimeEnd;

	public NewWebClientUrlObject(String url, String origin, String type, String referrer, Long referrerCookieId, Long contentId)
			throws URIException {
		super(url, origin, type);
		this.referrer = referrer;
		this.referrerCookieId = referrerCookieId;
		this.contentId = contentId;
	}

	public NewWebClientUrlObject(String url, String origin, String type, String mimeType, String referrer, Long referrerCookieId, Long contentId)
			throws URIException {
		this(url, origin, type, referrer, referrerCookieId, contentId);
		this.mimeType = mimeType.toLowerCase();
	}

	public NewWebClientUrlObject(String url, String origin, String type, String referrer, Long referrerCookieId) throws URIException {
		this(url, origin, type, referrer, referrerCookieId, null);
	}

	@Override
	public final ObjectData asDataObject(Long parentId) {
		ObjectDataBuilder objectBuilder = new ObjectDataBuilder();
		objectBuilder.addStringAttribute("type", getType());
		objectBuilder.addStringAttribute("url_original", getOriginalUrl());
		objectBuilder.addTimeAttribute("creation_time", System.currentTimeMillis());
		if (getOrigin() != null) {
			objectBuilder.addStringAttribute("origin", getOrigin());
		}
		if (getMimeType() != null) {
			objectBuilder.addStringAttribute("mime_type", getMimeType());
		}
		if (parentId != null) {
			objectBuilder.addObjAttribute("parent", parentId);
		}
		if (getReferrer() != null) {
			objectBuilder.addStringAttribute("referrer", getReferrer());
		}
		if (getReferrerCookieId() != null) {
			objectBuilder.addRefAttribute("referrer_cookie", DataStoreHelper.DEFAULT_STORE_ID, getReferrerCookieId());
		}
		if (getContentId() != null) {
			objectBuilder.addRefAttribute("content", DataStoreHelper.DEFAULT_STORE_ID, getContentId());
		}
		if (getDownloadTimeStart() != null) {
			objectBuilder.addTimeAttribute("download_time_start", getDownloadTimeStart());
		}
		if (getDownloadTimeEnd() != null) {
			objectBuilder.addTimeAttribute("download_time_end", getDownloadTimeEnd());
		}
		return objectBuilder.build();
	}

	public final String getReferrer() {
		return referrer;
	}

	public final Long getReferrerCookieId() {
		return referrerCookieId;
	}

	public final Long getContentId() {
		return contentId;
	}

	public final String getMimeType() {
		return mimeType;
	}

	public final Long getDownloadTimeStart() {
		return downloadTimeStart;
	}

	public final void setDownloadTimeStart(Long downloadTimeStart) {
		this.downloadTimeStart = downloadTimeStart;
	}

	public final Long getDownloadTimeEnd() {
		return downloadTimeEnd;
	}

	public final void setDownloadTimeEnd(Long downloadTimeEnd) {
		this.downloadTimeEnd = downloadTimeEnd;
	}


}
