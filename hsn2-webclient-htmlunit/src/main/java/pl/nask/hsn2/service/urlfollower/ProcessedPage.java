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
import java.net.URL;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.FrameWindow;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.UrlUtils;

public class ProcessedPage {
	private Page page;
	private int responseCode;
	private String responseHeader;
	private String contentType;
	private String requestHeader;
	private String serverSideRedirectLocation;
	private WebResponse response;
	private ProcessedPage clientSideRedirectPage;
	private URL requestedUrl;
	private URL actualUrl;
	private String originalUrl;
	private boolean fromFrame;

	public ProcessedPage(Page page) {
		this(page, "");
	}

	public ProcessedPage(Page page, ProcessedPage clientSideRedirectPage) {
		this(page, "");
		this.clientSideRedirectPage = clientSideRedirectPage;
	}

	public ProcessedPage(Page page, String originalUrl) {
		this.page = page;
		if (page != null) {
			response = page.getWebResponse();
			responseCode = response.getStatusCode();
			responseHeader = response.getResponseHeaders().toString();
			contentType = response.getContentType();
			checkServerSideRedirect();
			requestHeader = response.getWebRequest().getAdditionalHeaders().toString();
			requestedUrl = response.getWebRequest().getUrl();
			actualUrl = page.getUrl();
			fromFrame = page.getEnclosingWindow() instanceof FrameWindow;
		}
		this.originalUrl = originalUrl;
	}

	private void checkServerSideRedirect() {
		if (responseCode >= HttpStatus.SC_MULTIPLE_CHOICES && responseCode <= HttpStatus.SC_TEMPORARY_REDIRECT
				&& responseCode != HttpStatus.SC_NOT_MODIFIED) {
			// At this point response code is 300, 301, 302, 303, 305, 306 or 307 (but no 304).
			serverSideRedirectLocation = page.getWebResponse().getResponseHeaderValue("Location");

			// Server side redirection could be relative, so we have to make sure it is set correctly.
			URL requestUrl = page.getUrl();
			if (serverSideRedirectLocation == null || serverSideRedirectLocation.isEmpty()) {
				// It could happen location header is empty. This is not valid redirect.
				serverSideRedirectLocation = null;
			} else if (!(serverSideRedirectLocation.startsWith("http://") && serverSideRedirectLocation.startsWith("https://"))) {
				// It could happen location header is relative. This is not valid redirect but web
				// browsers seem to follow it, so WebClient do.
				serverSideRedirectLocation = UrlUtils.resolveUrl(requestUrl, serverSideRedirectLocation);
			}
		}
	}

	public InputStream getContentAsStream() {
		if (response == null) {
			return null;
		} else {
			return response.getContentAsStream();
		}
	}

	public boolean isHtml() {
		return page instanceof HtmlPage;
	}

	public int getResponseCode() {
		return responseCode;
	}

	public String getContentType() {
		return contentType;
	}

	public String getResponseHeaders() {
		return responseHeader;
	}

	public String getRequestHeaders() {
		return requestHeader;
	}

	public String getServerSideRedirectLocation() {
		return serverSideRedirectLocation;
	}

	public Page getPage() {
		return page;
	}

	public URL getRequestedUrl() {
		return requestedUrl;
	}

	public URL getActualUrl() {
		return actualUrl;
	}

	public void cleanPage() {
		if (response != null) {
			IOUtils.closeQuietly(response.getContentAsStream());
			response = null;
		}
	}

	public String getOriginalUrl() {
		if (originalUrl == null || originalUrl.isEmpty()) {
			return requestedUrl.toExternalForm();
		} else {
			return originalUrl;
		}
	}

	public ProcessedPage getClientSideRedirectPage() {
		return clientSideRedirectPage;
	}

	public ProcessedPage getLastPage() {
		if (clientSideRedirectPage != null) {
			return clientSideRedirectPage.getLastPage();
		} else {
			return this;
		}
	}

	public void stickChain(ProcessedPage chain) {
		if (clientSideRedirectPage == null) {
			page = chain.getPage();
			clientSideRedirectPage = chain.getClientSideRedirectPage();
			serverSideRedirectLocation = chain.getServerSideRedirectLocation();
		} else {
			clientSideRedirectPage.stickChain(chain);
		}
	}

	public void setClientSideRedirectPage(Page page) {
		clientSideRedirectPage = new ProcessedPage(page);
	}

	public boolean isFromFrame() {
		return fromFrame;
	}
	
	public boolean isComplete() {
		return page != null && response != null;
	}
}
