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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.client.utils.URIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Link {
	private static final Logger LOGGER = LoggerFactory.getLogger(Link.class);

	private final boolean decodeIIS;
	private static final Pattern PATTERN;
	static {
		PATTERN = Pattern.compile(".*%u[0-9a-fA-F]{4}.*");
	}

	private final URI absoluteUrl;
	private final String baseUrl;
	private final String relativeUrl;
	private final String append;

	// BitSet created for proper URL check
	public static final BitSet PROPER_URL_BITSET = new BitSet();
	static {
		for (int i = 'a'; i <= 'z'; i++) {
			PROPER_URL_BITSET.set(i);
		}
		for (int i = 'A'; i <= 'Z'; i++) {
			PROPER_URL_BITSET.set(i);
		}
		// numeric characters
		for (int i = '0'; i <= '9'; i++) {
			PROPER_URL_BITSET.set(i);
		}
		// blank to be replaced with +
		PROPER_URL_BITSET.set('-');
		PROPER_URL_BITSET.set('_');
		PROPER_URL_BITSET.set('.');
		PROPER_URL_BITSET.set(':');
		PROPER_URL_BITSET.set('/');
		PROPER_URL_BITSET.set('=');
		PROPER_URL_BITSET.set('?');
		PROPER_URL_BITSET.set('#');
	}

	public Link(String baseUrl, String relativeUrl) throws URISyntaxException {
		this(baseUrl, relativeUrl, false);
	}

	public Link(String baseUrl, String relativeUrl, boolean enableIISdecode) throws URISyntaxException {
		decodeIIS = enableIISdecode;
		this.baseUrl = baseUrl;
		URI baseURI = new URI(format(baseUrl));
		if (!decodeIIS && IISEncDec.isIISencoded(relativeUrl)) {
			this.relativeUrl = relativeUrl;
			int i = relativeUrl.indexOf("%u");
			String rel = format(relativeUrl.substring(0, i));
			append = format(relativeUrl.substring(i));
			if (rel.length() == 0) {
				rel = "/";
			}
			absoluteUrl = URIUtils.resolve(baseURI, rel);
			return;
		} else if (decodeIIS && IISEncDec.isIISencoded(relativeUrl)) {
			this.relativeUrl = IISEncDec.convertToUTF8(format(relativeUrl));
		} else {
			this.relativeUrl = relativeUrl;
		}
		append = "";
		try {
			absoluteUrl = URIUtils.resolve(baseURI, format(this.relativeUrl));
		} catch (IllegalArgumentException e) {
			LOGGER.debug("", e);
			throw new URISyntaxException("Cannot convert to absolute URL: " + relativeUrl, e.getCause().getMessage());
		}
	}

	protected Link(URL baseUrl, String relativeUrl) throws URISyntaxException {
		decodeIIS = false;
		this.baseUrl = baseUrl.toString();
		this.relativeUrl = relativeUrl;
		absoluteUrl = URIUtils.resolve(baseUrl.toURI(), format(relativeUrl));
		append = "";
	}

	private String format(String url) {
		return url.trim().replaceAll("[\u00A0\u0020]", "%20");
	}

	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (absoluteUrl == null ? 0 : absoluteUrl.hashCode());
		return result;
	}

	@Override
	public final boolean equals(Object obj) {
		// generated code
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Link other = (Link) obj;
		if (baseUrl == null) {
			if (other.baseUrl != null) {
				return false;
			}
		} else if (!baseUrl.equals(other.baseUrl)) {
			return false;
		}
		if (absoluteUrl == null) {
			if (other.absoluteUrl != null) {
				return false;
			}
		} else if (!absoluteUrl.equals(other.absoluteUrl)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "Link [baseUrl=" + baseUrl + ", absoluteUrl=" + absoluteUrl + ", relativeUrl=" + relativeUrl + "]";
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public String getAbsoluteUrl() {
		return absoluteUrl.toString() + append;
	}

	public String getRelativeUrl() {
		return relativeUrl;
	}

	public static class IISEncDec {
		private static final int NUMBER_16 = 16;
		private static final int NUMBER_4 = 4;
		private static final int NUMBER_6 = 6;

		public static String convertToUTF8(String urlPath) {
			if (!isIISencoded(urlPath)) {
				return urlPath;
			}
			int i = urlPath.indexOf("%u");
			StringBuilder sb = new StringBuilder(urlPath);
			if (i >= 0) {
				ByteBuffer bb = ByteBuffer.allocate(NUMBER_4);
				while ((i = sb.indexOf("%u", i)) >= 0) {
					if (i + NUMBER_6 >= sb.length()) {
						break;
					}
					Integer val = Integer.parseInt(sb.substring(i + 2, i + NUMBER_6), NUMBER_16);
					bb.putInt(val);
					try {
						byte b[] = ArrayUtils.subarray(bb.array(), 2, NUMBER_4);
						String s = URLEncoder.encode(new String(b, "UTF-16"), "UTF-8");
						sb.replace(i, i + NUMBER_6, s);
						bb.rewind();
					} catch (UnsupportedEncodingException e) {
						i++;
					}
				}
			}
			return sb.toString();
		}

		public static boolean isIISencoded(String relUrl) {
			return PATTERN.matcher(relUrl).matches();
		}
	}
}