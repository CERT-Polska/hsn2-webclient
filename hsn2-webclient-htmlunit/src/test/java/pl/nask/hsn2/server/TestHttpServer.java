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
import java.net.ServerSocket;

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.MimeTypes;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.handler.MovedContextHandler;
import org.mortbay.jetty.handler.ResourceHandler;

public class TestHttpServer {
	private static Server webserver;
	private static final int DEFAULT_WEBSERVER_PORT = 8123;
	private static int webserverPort;
	public static final String LOCAL_RESOURCE_PATH = "./src/test/resources/webtest";

	public static void startServer() throws Exception {
		startServer("");
	}

	/**
	 * Start a server using specified subdir of a WEBSERVER_ROOT as a resource base. 
	 * @param resourceDir a subdir to be used
	 * @throws Exception
	 */
	public static void startServer(String resourceDir) throws Exception {
		stopServer();
		updateSocketNumber();
		if (webserver != null && !webserver.isStopped()) {
			webserver.stop();
		}
		webserver = new Server(webserverPort);

		ResourceHandler rh = new ResourceHandler();
		rh.setWelcomeFiles(new String[] { "index.html" });
		rh.setResourceBase(LOCAL_RESOURCE_PATH + "/" + resourceDir);
		MimeTypes mimeTypes = new MimeTypes();
		mimeTypes.addMimeMapping("swf", "application/x-shockwave-flash");
		rh.setMimeTypes(mimeTypes);

		TemplateHandler wrappedRh = new TemplateHandler();
		wrappedRh.setHandler(rh);
		wrappedRh.addSubstitution("WEBSERVER_PORT", "" + webserverPort);
		wrappedRh.addSubstitution("WEBSERVER_ADDRESS", getWebserverAddress());

		MovedContextHandler redirectHandler = new MovedContextHandler();
		redirectHandler.setContextPath("/movedContext");
		redirectHandler.setNewContextURL("/newLocation");
		HandlerList h = new HandlerList();

		// Note: wrappedRh handler will handle every request to html/htm file
		// and will not respond with any error (i.e. you will never get 404
		// response). If you need to test proper 404 response behavior you
		// should use different file type in request (i.e. noSuchFile.txt).

		h.setHandlers(new Handler[] { new MyCookieHandler(), redirectHandler, wrappedRh, new DefaultHandler() });
		webserver.setHandler(h);
		webserver.start();
	}

	private static void updateSocketNumber() {
		webserverPort = DEFAULT_WEBSERVER_PORT;
		ServerSocket s = null;
		while (s == null) {
			try {
				s = new ServerSocket(webserverPort);
			} catch (Exception e) {
				webserverPort++;
			} finally {
				if (s != null) {
					// right socket found!
					try {
						s.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
	}

	public static void stopServer() throws Exception {
		if (webserver != null)
			webserver.stop();
	}

	public static String absoluteUrl(String relativePath) {
		return getWebserverRoot() + relativePath;
	}

	public static int getWebserverPort() {
		return webserverPort == 0 ? DEFAULT_WEBSERVER_PORT : webserverPort;
	}

	public static String getWebserverAddress() {
		return "http://localhost:" + getWebserverPort();
	}

	public static String getWebserverRoot() {
		return getWebserverAddress() + "/";
	}
}
