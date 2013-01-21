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

package pl.nask.hsn2.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.handler.HandlerWrapper;

public class TemplateHandler extends HandlerWrapper implements Handler {

	private Map<String, String> substitutions = new HashMap<String, String>();

	@Override
	public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException {

		if (target.endsWith("html") || target.endsWith("htm")) {
			HttpServletResponseWrapper wrapper = new TemplatedResponseWrapper(response, substitutions);
			super.handle(target, request, wrapper, dispatch);
			wrapper.flushBuffer();
		} else if (target.endsWith("ssrAbsolute")) {
			// For absolute/relative server side redirects tests (refs #8333)
			response.sendRedirect(TestHttpServer.absoluteUrl("realpage.html"));
		} else if (target.endsWith("ssrRelativeToSeverRoot")) {
			// For absolute/relative server side redirects tests (refs #8333)
			String newTarget = "/realpage.html";
			HttpServletResponseWrapper wrapper = new HttpServletResponseWrapper(response);
			response.setHeader("Location", newTarget);
			response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
			super.handle(newTarget, request, wrapper, dispatch);
			wrapper.flushBuffer();
		} else if (target.endsWith("ssrRelativeToDirectory")) {
			// For absolute/relative server side redirects tests (refs #8333)
			String newTarget = "realpage.html";
			HttpServletResponseWrapper wrapper = new HttpServletResponseWrapper(response);
			response.setHeader("Location", newTarget);
			response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
			super.handle(newTarget, request, wrapper, dispatch);
			wrapper.flushBuffer();
		} else if (target.endsWith("ssrEmptyLocation")) {
			// For server side redirect to empty location test (refs #8702)
			HttpServletResponseWrapper wrapper = new HttpServletResponseWrapper(response);
			response.setHeader("Location", "");
			response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
			super.handle("", request, wrapper, dispatch);
			wrapper.flushBuffer();
		} else if (target.endsWith("ssrNoLocation")) {
			// For server side redirect to empty location test (refs #8702)
			HttpServletResponseWrapper wrapper = new HttpServletResponseWrapper(response);
			response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
			super.handle("", request, wrapper, dispatch);
			wrapper.flushBuffer();
		} else {
			super.handle(target, request, response, dispatch);
		}
	}

	public void addSubstitution(String key, String value) {
		this.substitutions.put(key, value);
	}
}
