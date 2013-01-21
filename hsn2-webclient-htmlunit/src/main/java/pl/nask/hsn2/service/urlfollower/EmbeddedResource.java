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

import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.io.IOUtils;

import pl.nask.hsn2.service.urlfollower.PageLinks.LinkType;

import com.gargoylesoftware.htmlunit.WebResponse;

public class EmbeddedResource extends Link {
	private LinkType linkType;
	
    private String contentType;
    private Integer responseCode;
    private String failureMessage;
    private InputStream stream;
    private boolean requestFailed;
	private String requestHeader;
	private String responseHeader;
	private URL currUrl;

	public EmbeddedResource(String baseUrl, String srcAttrValue, LinkType type) throws URISyntaxException {		
        super(baseUrl, srcAttrValue);
        this.linkType = type;
    }

    public EmbeddedResource(String baseUrl, String srcAttrValue, String failureMessage, boolean requestFailed, String requestHeader) throws URISyntaxException {
        super(baseUrl, srcAttrValue);
        this.failureMessage = failureMessage;
        this.requestFailed = requestFailed;
        this.requestHeader = requestHeader;
    }

    public void update(String type, InputStream stream, Integer responseCode, String failureMessage, boolean requestFailed) {
        this.contentType = type;
        this.stream = stream;
        this.responseCode = responseCode;
        this.failureMessage = failureMessage;
        this.requestFailed = requestFailed;
    }

    public void update(WebResponse webResponse, String failureMessage, boolean requestFailed) {
    	if ( webResponse !=null) {
    		this.contentType = webResponse.getContentType();
    		this.stream = webResponse.getContentAsStream();
    		this.responseCode = webResponse.getStatusCode();
    		this.requestHeader = webResponse.getWebRequest().getAdditionalHeaders().toString();
    		this.responseHeader = webResponse.getResponseHeaders().toString();
    		this.currUrl = webResponse.getWebRequest().getUrl();
    	}
    	this.failureMessage = failureMessage;
    	this.requestFailed = requestFailed;
    }

    public String getContentType() {
        return contentType;
    }

    public Integer getResponseCode() {
        return responseCode;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public InputStream getContentStream() {
        return stream;
    }
    
    public boolean isRequestFailed(){
    	return requestFailed;
    }

    public String getRequestHeader() {
		return requestHeader;
	}

	public String getResponseHeader() {
		return responseHeader;
	}
	
	public LinkType getLinkType() {
		return linkType;
	}

	public void closeStream() {
		IOUtils.closeQuietly(stream);
	}
	
	@Override
	public String getAbsoluteUrl() {
		if ( this.currUrl == null) {
			return super.getAbsoluteUrl();
		}
		return currUrl.toExternalForm();
		
	}

	public String getAbsoluteUrlBeforeRedirect() {
		return super.getAbsoluteUrl();
	}
	
	public boolean isServerSideRedirect() {
		return currUrl == null ? false :!super.getAbsoluteUrl().equals(getAbsoluteUrl());
	}
}
