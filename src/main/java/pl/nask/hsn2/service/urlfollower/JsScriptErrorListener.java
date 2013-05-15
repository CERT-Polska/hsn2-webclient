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

import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gargoylesoftware.htmlunit.ScriptException;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.JavaScriptErrorListener;

public class JsScriptErrorListener implements JavaScriptErrorListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(JsScriptErrorListener.class);

	@Override
	public void scriptException(HtmlPage htmlPage, ScriptException scriptException) {
		if (scriptException == null || scriptException.getMessage() == null) {
			LOGGER.warn("Strange ScriptException caught:{}", htmlPage.getUrl());
			return;
		}
		LOGGER.warn("ScriptException {} ; {}", htmlPage.getUrl(), scriptException.getMessage());
		if (scriptException.getMessage().contains("java.lang.RuntimeException")
				|| scriptException.getMessage().contains("Too deep recursion while parsing")) {
			LOGGER.warn("Serious problem in JavaScript engine: {}", scriptException.getMessage());
			LOGGER.debug("Serious problem in JavaScript engine", scriptException);
		} else {
			LOGGER.warn("Other problem in JavaScript engine: {}", scriptException.getMessage());
			LOGGER.debug("Other problem in JavaScript engine", scriptException);
		}
		{
		//htmlPage.getWebClient().getOptions().setJavaScriptEnabled(false);
//		htmlPage.getWebClient().getOptions().setJavaScriptEnabled(true);
		
		}
		
	}

	@Override
	public void timeoutError(HtmlPage htmlPage, long allowedTime, long executionTime) {
		LOGGER.debug("JS script timeout (timeout set: {}, execution time: {}).", allowedTime, executionTime);
	}

	@Override
	public void malformedScriptURL(HtmlPage htmlPage, String url, MalformedURLException malformedURLException) {
		LOGGER.debug("Malformed URL:{}.", url);
	}

	@Override
	public void loadScriptError(HtmlPage htmlPage, URL scriptUrl, Exception exception) {
		LOGGER.warn("Error loading script '{}' because: {}", scriptUrl, exception.getMessage());
		LOGGER.debug("Error loading script (stacktrace):", exception);
	}
}
