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

import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.httpclient.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebWindowEvent;
import com.gargoylesoftware.htmlunit.WebWindowListener;
import com.gargoylesoftware.htmlunit.html.FrameWindow;

public class WebWindowListenerImpl implements WebWindowListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebWindowListenerImpl.class);
	private Map<Page, ProcessedPage> previousTopPageMap;
	private Map<Page, ProcessedPage> previousFramePageMap;

	public WebWindowListenerImpl(Map<Page, ProcessedPage> previousTopPageMap, Map<Page, ProcessedPage> previousFramePageMap) {
		this.previousTopPageMap = previousTopPageMap;
		this.previousFramePageMap = previousFramePageMap;
	}

	@Override
	public final void webWindowOpened(WebWindowEvent event) {
		LOGGER.debug("WebWindow opened:{}->{}",event.getOldPage().getUrl(),event.getNewPage().getUrl());
	}

	@Override
	public final void webWindowContentChanged(WebWindowEvent event) {
		Page oldPage = event.getOldPage();
		Page newPage = event.getNewPage();
		LOGGER.debug("WebWindow opened:{}->{}",oldPage,newPage.getUrl());
		if (oldPage == null || !newPage.getUrl().toExternalForm().equals(oldPage.getUrl().toExternalForm())) {
			checkWhenAddressIsTheSame(event, oldPage, newPage);
		} else {
			checkWhenAddressIsDifferent(oldPage, newPage);
		}
	}

	private void checkWhenAddressIsDifferent(final Page oldPage, final Page newPage) {
		ProcessedPage unchanged = previousTopPageMap.remove(oldPage);

		if (unchanged != null) {
			for (Entry<Page, ProcessedPage> entry : previousTopPageMap.entrySet()) {
				ProcessedPage actualPage = entry.getValue();

				if (actualPage.getPage() == newPage) {
					if (oldPage instanceof TextPage) {
						LOGGER.warn("Probably server redirect to the same page {} --> {}. Chain was cut.", oldPage.getUrl(), newPage.getUrl());
					} else {
						unchanged.stickChain(actualPage);
						previousTopPageMap.put(entry.getKey(), unchanged);
						LOGGER.debug("Page {} was sticked with {}", oldPage.getUrl(), entry.getKey().getUrl());
					}
					return;
				}
			}
			unchanged.stickChain(new ProcessedPage(newPage));
			previousTopPageMap.put(newPage, unchanged);
			LOGGER.debug("Page {} was sticked with {}", oldPage.getUrl(), newPage.getUrl());
		}
	}

	private void checkWhenAddressIsTheSame(WebWindowEvent event, final Page oldPage, final Page newPage) {
		// Old page not exists or old and new URLs are the same
		if (event.getWebWindow() instanceof FrameWindow) {
			checkFrameWindow(oldPage, newPage);
		} else {
			checkNonFrameWindow(oldPage, newPage);
		}
	}

	private void checkNonFrameWindow(final Page oldPage, final Page newPage) {
		for (Entry<Page, ProcessedPage> entry : previousTopPageMap.entrySet()) {
			ProcessedPage actualPage = entry.getValue();

			if (actualPage.getPage() == newPage) {
				if (oldPage == null || isServerRedirect(oldPage)) {
					LOGGER.debug("Root or after server redirect page was reached in main window: {}", newPage.getUrl());
				} else {
					previousTopPageMap.put(entry.getKey(), new ProcessedPage(oldPage, actualPage));
					LOGGER.debug("New client redirect detected for {} : {} --> {}",
							new Object[] { entry.getKey(), oldPage.getUrl(), actualPage.getActualUrl() });
				}
				return;
			}
		}
		if (oldPage == null || isServerRedirect(oldPage)) {
			previousTopPageMap.put(newPage, new ProcessedPage(newPage));
			LOGGER.debug("Root page was displayed in main window: {}", newPage.getUrl());
		} else {
			if (previousTopPageMap.containsKey(oldPage)) {
				ProcessedPage oldChain = previousTopPageMap.remove(oldPage);
				oldChain.getLastPage().setClientSideRedirectPage(newPage);
				previousTopPageMap.put(newPage, oldChain);
			} else {
				previousTopPageMap.put(newPage, new ProcessedPage(oldPage, new ProcessedPage(newPage)));
				LOGGER.debug("New client redirect detected for {} : {} --> {}", new Object[] { newPage.getUrl(), oldPage.getUrl(), newPage.getUrl() });
			}
		}
	}

	private void checkFrameWindow(final Page oldPage, final Page newPage) {
		if (oldPage != null) {
			for (Entry<Page, ProcessedPage> entry : previousFramePageMap.entrySet()) {
				ProcessedPage actualPage = entry.getValue();

				if (actualPage.getPage() == newPage) {
					if ("about:blank".equals(oldPage.getUrl().toExternalForm()) || isServerRedirect(oldPage)) {
						LOGGER.debug("Root page was reached in frame window: {}", newPage.getUrl());
					} else {
						previousFramePageMap.put(entry.getKey(), new ProcessedPage(oldPage, actualPage));
						LOGGER.debug("New client redirect in frame detected for {} : {} --> {}", new Object[] { entry.getKey(), oldPage.getUrl(),
								actualPage.getActualUrl() });
					}
					return;
				}
			}
			if ("about:blank".equals(oldPage.getUrl().toExternalForm()) || isServerRedirect(oldPage)) {
				previousFramePageMap.put(newPage, new ProcessedPage(newPage));
				LOGGER.debug("Root page was displayed in frame window: {}", newPage.getUrl());
			} else {
				if (previousFramePageMap.containsKey(oldPage)) {
					ProcessedPage oldChain = previousFramePageMap.remove(oldPage);
					oldChain.getLastPage().setClientSideRedirectPage(newPage);
					previousFramePageMap.put(newPage, oldChain);
				} else {
					previousFramePageMap.put(newPage, new ProcessedPage(oldPage, new ProcessedPage(newPage)));
					LOGGER.debug("New client redirect in frame detected for {} : {} --> {}",
							new Object[] { newPage.getUrl(), oldPage.getUrl(), newPage.getUrl() });
				}
			}
		}
	}

	@Override
	public final void webWindowClosed(WebWindowEvent event) {
		Page oldPage = event.getOldPage();
		if (oldPage != null && !"about:blank".equals(oldPage.getUrl().toExternalForm())) {
			if (event.getWebWindow() instanceof FrameWindow) {
				if (!previousFramePageMap.containsKey(oldPage)) {
					previousFramePageMap.put(oldPage, new ProcessedPage(oldPage, new ProcessedPage(oldPage)));
					LOGGER.debug("New client redirect in frame detected when window close {} ", oldPage.getUrl());
				}
			} else {
				if (!previousTopPageMap.containsKey(oldPage)) {
					previousTopPageMap.put(oldPage, new ProcessedPage(oldPage, new ProcessedPage(oldPage)));
					LOGGER.debug("New client redirect detected when window closed for {} ", oldPage.getUrl());
				}
			}
		}
	}

	private boolean isServerRedirect(Page page) {
		int responseCode = page.getWebResponse().getStatusCode();
		// returns 300, 301, 302, 303, 305, 306 or 307 (but no 304).
		return responseCode >= HttpStatus.SC_MULTIPLE_CHOICES && responseCode <= HttpStatus.SC_TEMPORARY_REDIRECT && responseCode != HttpStatus.SC_NOT_MODIFIED;
	}
}
