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

package pl.nask.hsn2.service;


import pl.nask.hsn2.wrappers.ObjectDataWrapper;

public class ServiceData {
    private static final int LAST_SLASH_BOUNDARY = 8;
	private Long inputUrlId;
    private String inputUrlNormalized;
    private String inputUrlOriginal;
    private String inputReferrer;
    private Long inputReferrerCookieId;
    private Integer depth;
    private Long topAncestorId;
    private String urlForProcessing;
    private String proxy;

    ServiceData(String originalUrl, String normalizedUrl) {
    	// constructor for tests only
    	setInputUrlOriginal(originalUrl);
        inputUrlNormalized = normalizedUrl;
    }

	public ServiceData(Long inputUrlId, String inputUrlNormalized, String inputUrlOriginal, String inputReferrer,
			Long inputReferrerCookieId, Integer depth, Long topAncestorId,String proxyUri) {
		this.inputUrlId = inputUrlId;
		this.inputUrlNormalized = inputUrlNormalized;
		setInputUrlOriginal(inputUrlOriginal);
		this.inputReferrer = inputReferrer;
		this.inputReferrerCookieId = inputReferrerCookieId;
		this.depth = depth;
		this.topAncestorId = topAncestorId;
		this.proxy = proxyUri;
	}

    public ServiceData(ObjectDataWrapper objectData) {
        this.inputUrlNormalized = objectData.getUrlNormalized();
        setInputUrlOriginal(objectData.getUrlOriginal());
        this.inputReferrer = objectData.getString("referrer");
        this.inputReferrerCookieId = objectData.getReferenceId("referrer_cookie");
        this.inputUrlId = objectData.getId();
        this.depth = objectData.getInt("depth");
        this.topAncestorId = objectData.getObjectId("top_ancestor");
        this.proxy = objectData.getString("proxy");
    }

    public String getInputReferrer() {
        return inputReferrer;
    }

    public Long getInputReferrerCookieId() {
        return inputReferrerCookieId;
    }

    public String getInputUrlNormalized() {
        return inputUrlNormalized;
    }

    public String getInputUrlOriginal() {
        return inputUrlOriginal;
    }

    public Integer getDepth() {
        return depth;
    }

    public Long getTopAncestorId() {
        return topAncestorId;
    }

    public Long getInputUrlId() {
        return inputUrlId;
    }

    public String getUrlForProcessing() {
    	return urlForProcessing;
    }
    public String getProxyUri() {
    	return proxy;
    }

	public ServiceData getServiceDataCopyForNewSubcontext(String newUrlOriginal, String newReferrer, Long newReferrerCookieId) {
		return new ServiceData(inputUrlId, inputUrlNormalized, newUrlOriginal, newReferrer, newReferrerCookieId, depth, topAncestorId, proxy);
	}

	private void setInputUrlOriginal(String s) {
		this.inputUrlOriginal = s.intern();
		setUrlForProcessing();
	}

	/**
	 * Sets urlForProcessing based on inputUrlOriginal.
	 */
	private void setUrlForProcessing() {
		StringBuilder sb = new StringBuilder(inputUrlOriginal.replaceAll("[\u00A0\u0020]", "%20"));
		if (isContainingHostnameOnly(sb.toString())) {
			sb.append('/');
		}
		urlForProcessing = sb.toString().intern();
	}

	/**
	 * Returns true if given string starts with http|https and
	 * contains only hostname without trailing slash. Does not
	 * check if URL is legal - only checks for trailing slash.
	 * 
	 * @return True if URL is only host name and is not ending with slash.
	 */
	private boolean isContainingHostnameOnly(String url) {
		boolean result = false;
		if (url.startsWith("http://") || url.startsWith("https://")) {
			int lastSlashIndex = url.lastIndexOf('/');
			char lastCharacter = url.charAt(url.length() - 1);
			if (lastSlashIndex < LAST_SLASH_BOUNDARY && lastCharacter != '/') {
				return true;
			}
		}
		return result;
	}

	@Override
	public String toString() {
		String s = ServiceData.class.getSimpleName() + "{"
		+ "inputUrlId=" + inputUrlId
		+ ",inputUrlOriginal=" + inputUrlOriginal
		+ ",inputUrlNormalized=" + inputUrlNormalized
		+ ",inputReferrer=" + inputReferrer
		+ ",inputReferrerCookieId=" + inputReferrerCookieId
		+ ",topAncestorId=" + topAncestorId
		+ ",depth=" + depth
		+ ",urlForProcessing=" + urlForProcessing
		+ "}";
		return s;
	}
}
