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
import java.util.Map;

import javax.servlet.ServletOutputStream;


public class TemplatedServletOutputStream extends ServletOutputStream {	
	
	private ServletOutputStream outputStream;
	private StringBuilder stringBuilder;
	private Map<String, String> substitutions;

	public TemplatedServletOutputStream(ServletOutputStream outputStream, Map<String, String> substitutions) {
		this.substitutions = substitutions;
		this.stringBuilder = new StringBuilder(1024);
		this.outputStream = outputStream;
	}

	@Override
	public void write(int b) throws IOException {
		if (b < Character.MIN_VALUE || b > Character.MAX_VALUE)
			throw new IOException("Cannot cast " + b + " to a char");
		if (stringBuilder == null) {
			throw new IOException("Closed");
		}
		stringBuilder.append((char) b);
	}
	
	public String doFilter() {
		String original = stringBuilder.toString();
		for (Map.Entry<String, String> e: substitutions.entrySet()) {
			original = original.replaceAll("\\$\\{" + e.getKey() + "}", e.getValue());
		}
		return original;
	}
	
	@Override
	public void flush() throws IOException {
		String output = doFilter();
		stringBuilder = new StringBuilder(1024);
		outputStream.print(output);
		outputStream.flush();
	}
	
	@Override
	public void close() throws IOException {
		if (stringBuilder != null) {
			flush();
			stringBuilder = null;
			outputStream.close();			
		}
	}
}
