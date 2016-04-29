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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.WebWindow;
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
	private WebWindow webWindow;
	private String asString  = null;

	private static final Logger LOGGER = LoggerFactory.getLogger(ProcessedPage.class);

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
			webWindow = page.getEnclosingWindow() ;
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

	public final InputStream getContentAsStream() {
		if (response == null) {
			return null;
		} else {
			try {
				return response.getContentAsStream();
			} catch (IOException e) {
				LOGGER.error(e.getMessage(), e);
				return null;
			}
		}
	}

	public final boolean isHtml() {
		return page instanceof HtmlPage;
	}

	public final int getResponseCode() {
		return responseCode;
	}

	public final String getContentType() {
		return contentType;
	}

	public final String getResponseHeaders() {
		return responseHeader;
	}

	public final String getRequestHeaders() {
		return requestHeader;
	}

	public final String getServerSideRedirectLocation() {
		return serverSideRedirectLocation;
	}

	public final Page getPage() {
		return page;
	}

	public final URL getRequestedUrl() {
		return requestedUrl;
	}

	public final URL getActualUrl() {
		return actualUrl;
	}

	public final void cleanPage() {
		if (response != null) {
			try {
				IOUtils.closeQuietly(response.getContentAsStream());
//				webWindow = null;
			} catch (IOException e) {
				LOGGER.error("Error while cleaning page.", e);
			}
			response = null;
		}
	}

	public final String getOriginalUrl() {
		if (originalUrl == null || originalUrl.isEmpty()) {
			return requestedUrl.toExternalForm();
		} else {
			return originalUrl;
		}
	}

	public final ProcessedPage getClientSideRedirectPage() {
		return clientSideRedirectPage;
	}

	public final ProcessedPage getLastPage() {
		if (clientSideRedirectPage != null) {
			return clientSideRedirectPage.getLastPage();
		} else {
			return this;
		}
	}

	public final void stickChain(ProcessedPage chain) {
		if (clientSideRedirectPage == null) {
			page = chain.getPage();
			clientSideRedirectPage = chain.getClientSideRedirectPage();
			serverSideRedirectLocation = chain.getServerSideRedirectLocation();
		} else {
			clientSideRedirectPage.stickChain(chain);
		}
	}

	public final void setClientSideRedirectPage(Page page) {
		clientSideRedirectPage = new ProcessedPage(page);
	}

	public final boolean isFromFrame() {
		return fromFrame;
	}

	public final boolean isComplete() {
		return page != null && response != null;
	}
	@Override
	public final String toString() {
		if (asString == null) {
			StringBuilder sb = new StringBuilder();
			sb.append(responseCode).append(".");
			sb.append("(type=").append(webWindow.getClass().getSimpleName()).append(")").append(".");
			sb.append(originalUrl).append("->");
			sb.append(actualUrl).append(".");
			asString = sb.toString();
		}
		return asString;
	}
}
