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

package pl.nask.hsn2.server;

import java.io.IOException;
import java.net.ServerSocket;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ServerTest {

	@Test
	public void testServerStartsWithSocketConflict() throws Exception {
		ServerSocket s = null;		
		try {
			s = tryToOpenServerSocket();
			TestHttpServer.startServer();
		} finally {
			if ( s!= null) {
				s.close();
			}
			TestHttpServer.stopServer();
		}
	}

	private ServerSocket tryToOpenServerSocket() {
		try {
			// make sure that the server port is already in use
			return new ServerSocket(TestHttpServer.getWebserverPort());
		} catch (IOException e) {
			// address already in use, that's ok.
			return null;
		}
	}

	@Test
	public void testServerMayBeStoppedMultipleTimes() throws Exception {
		try {
			TestHttpServer.startServer();
		} finally {
			TestHttpServer.stopServer();
			TestHttpServer.stopServer();
			TestHttpServer.stopServer();
			TestHttpServer.stopServer();
			TestHttpServer.stopServer();
		}
	}
	
	@Test
	public void testServerWithTemplating() throws Exception {
		try {			
			TestHttpServer.startServer("serverTests");
			String url = TestHttpServer.absoluteUrl("template.html");
			HttpClient client = new HttpClient();
			GetMethod method = new GetMethod(url);
			client.executeMethod(method);
			String response = method.getResponseBodyAsString();
			Assert.assertEquals(response, "port:" + TestHttpServer.getWebserverPort() + ",address="+TestHttpServer.getWebserverAddress());
		} finally {
			TestHttpServer.stopServer();
		}
		
	}
}
