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
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class TemplatedResponseWrapper extends HttpServletResponseWrapper {

	private TemplatedServletOutputStream outputStream;
	private PrintWriter writer;
	private Map<String, String> substitutions;

	public TemplatedResponseWrapper(HttpServletResponse response, Map<String, String> substitutions) {
		super(response);
		this.substitutions = substitutions;
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		if (outputStream == null) {
			outputStream = new TemplatedServletOutputStream(super.getOutputStream(), substitutions);
		}
		
		return outputStream;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		
		if (outputStream != null)
			throw new IllegalStateException("getOutputStream() already called");
		
		if (writer != null) {
			writer = super.getWriter();
		}
		
		return writer;
	}
	
	@Override
	public void flushBuffer() throws IOException {
		if (outputStream != null) {
			// TODO: not very effective
			String preparedResponse = outputStream.doFilter();
			getResponse().setContentLength(preparedResponse.length());
			outputStream.flush();
		}
		super.flushBuffer();
	}
}
