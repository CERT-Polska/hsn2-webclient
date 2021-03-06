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

package pl.nask.hsn2.wrappers;

import pl.nask.hsn2.RequiredParameterMissingException;
import pl.nask.hsn2.service.urlfollower.EmbeddedResource;

public class RequestWrapper {

    private String originalUrl;
    private String absoluteUrl;
    private String requestHeader;
    private Integer responseCode;
    private String responseHeader;

    public RequestWrapper(String originalUrl, String absoluteUrl, String requestHeader) throws RequiredParameterMissingException {
    	this.originalUrl = originalUrl;
        this.absoluteUrl = absoluteUrl;
        this.requestHeader = requestHeader;
        validate();
    }

    public RequestWrapper(EmbeddedResource resource) throws RequiredParameterMissingException {
    	originalUrl = resource.getAbsoluteUrl();
        absoluteUrl = resource.getAbsoluteUrl();
        requestHeader = resource.getRequestHeader();
        responseCode =  resource.getResponseCode();
        responseHeader = resource.getResponseHeader();
        validate();
	}

    public RequestWrapper(String originalUrl, String absoluteUrl, String requestHeader, Integer responseCode, String responseHeader) throws RequiredParameterMissingException {
        this.originalUrl = originalUrl;
        this.absoluteUrl = absoluteUrl;
        this.requestHeader = requestHeader;
        this.responseCode = responseCode;
        this.responseHeader = responseHeader;
        validate();
    }

	private void validate() throws RequiredParameterMissingException {
		if (originalUrl == null) {
			throw new RequiredParameterMissingException("request_url_original");
		}
        if (absoluteUrl == null) {
        	throw new RequiredParameterMissingException("request_url_absolute");
        }
	}

	public final String getOriginalUrl() {
        return originalUrl;
    }

    public final String getAbsoluteUrl() {
        return absoluteUrl;
    }

    public final String getRequestHeader()  {
        return requestHeader;
    }

    public final Integer getResponseCode() {
        return responseCode;
    }

    public final String getResponseHeader() {
        return responseHeader;
    }

	@Override
	public final String toString() {
		return "RequestWrapper [orgUrl=" + originalUrl + ", absUrl=" + absoluteUrl + ", reqHeader=" + requestHeader + ", respCode=" + responseCode
				+ ", respHeader=" + responseHeader + "]";
	}


}
