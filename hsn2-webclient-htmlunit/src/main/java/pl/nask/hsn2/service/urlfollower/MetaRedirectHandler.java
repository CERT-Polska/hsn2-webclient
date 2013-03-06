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

import java.io.IOException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.RefreshHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebWindow;

public class MetaRedirectHandler implements RefreshHandler {
	private static final int ONE_SECOND_IN_MILISECONDS = 1000;
	private static final Logger LOGGER = LoggerFactory.getLogger(MetaRedirectHandler.class);
	private int miliSecTimeout;
	private int refreshCounter = 0;
	private final int refreshCountLimit;

	public MetaRedirectHandler(int miliSecTimeout, int refreshCountLimit) {
		this.miliSecTimeout = miliSecTimeout;
		this.refreshCountLimit = refreshCountLimit;
	}

	@Override
	public void handleRefresh(Page page, URL url, int seconds) throws IOException {
		refreshCounter++;
		LOGGER.debug("Meta refresh counter = {}", refreshCounter);
		if (refreshCounter >= refreshCountLimit) {
			LOGGER.debug("Meta refresh counter limit reached.");
			return;
		}
		if (page.getWebResponse().getWebRequest().getUrl().toExternalForm().equals(url.toExternalForm())
				&& HttpMethod.GET == page.getWebResponse().getWebRequest().getHttpMethod()) {
			LOGGER.debug("Refresh was interrupted: Redirect to itself.");
		} else {
			if (miliSecTimeout / ONE_SECOND_IN_MILISECONDS >= seconds) {
				if (seconds > 0) {
					try {
						Thread.sleep(seconds * ONE_SECOND_IN_MILISECONDS);
					} catch (final InterruptedException e) {
						LOGGER.debug("No big deal: Waiting thread was interrupted.", e);
					}
				}
				final WebWindow window = page.getEnclosingWindow();
				if (window != null) {
					final WebClient client = window.getWebClient();
					client.getPage(window, new WebRequest(url));
					if (!page.getUrl().toExternalForm().equals(url.toExternalForm())) {
						LOGGER.debug("Client side (meta-refresh) redirect occured. New location : {}", url.toExternalForm());
					}
				}
			} else {
				LOGGER.debug("Refresh to {} was interrupted: PageTimeout({}) was lower then refresh ({}).",
						new Object[] { url.toExternalForm(), miliSecTimeout / ONE_SECOND_IN_MILISECONDS, seconds });
			}
		}
	}
}
