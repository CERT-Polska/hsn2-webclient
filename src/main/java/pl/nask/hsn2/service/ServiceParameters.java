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

import pl.nask.hsn2.ParameterException;
import pl.nask.hsn2.wrappers.ParametersWrapper;

public class ServiceParameters {

	// Default values
	public static final int BACKGROUND_JS_TIMEOUT = 10000; 	private int backgroundJsTimeoutMillis;
	public static final int PAGE_TIMEOUT = 30000; 			private int pageTimeoutMillis;
	public static final int PROCESSING_TIMEOUT = 90000;		private int processingTimeout;
	public static final int SINGLE_JS_TIMEOUT = 3000; private int singleJsTimeoutMillis;

	public static final boolean ADD_REFERRER_COOKIE = true; 	private boolean addReferrerCookie;
	public static final boolean ADD_REFERRER = true;  		private boolean addReferrer;
	public static final boolean SAVE_JS_CONTEXT = true;		private boolean saveJsContext;
	public static final boolean SAVE_COOKIES = true; 		private boolean saveCookies;
	public static final boolean SAVE_HTML = true;  			private boolean saveHtml;
	public static final boolean SAVE_FAILED = true; 		private boolean saveFailed;
	public static final boolean SAVE_IMAGES = false; 		private boolean saveImages;
	public static final boolean SAVE_MULTIMEDIA = false; 	private boolean saveMultimedia;
	public static final boolean SAVE_OBJECTS = false; 		private boolean saveObjects;
	public static final boolean SAVE_OTHERS = false; 		private boolean saveOthers;
	public static final int REDIRECT_DEPTH_LIMIT = 10; 		private int redirectDepthLimit;
	public static final int REDIRECT_TOTAL_LIMIT = 50;  private int redirectTotalLimit;
	public static final int LINK_LIMIT = 100;			private int linkLimit;
	public static final int LINK_CLICK_POLICY = 1;		private int processExternalLinks;
	public static final int JS_RECURSION_LIMIT = 80;	private int jsRecursionLimit;
	public static final boolean JS_ENABLE = true;		private boolean jsEnable;

	/**
	 * default constructor means, that only default parameters should be used
	 * 
	 * @throws ParameterException
	 */
	public ServiceParameters() throws ParameterException {
		this(new ParametersWrapper());
	}

	public ServiceParameters(ParametersWrapper params) throws ParameterException {
		processExternalLinks = params.getInt("link_click_policy", LINK_CLICK_POLICY);
		linkLimit = params.getInt("link_limit", LINK_LIMIT);
		redirectTotalLimit = params.getInt("redirect_total_limit", REDIRECT_TOTAL_LIMIT);
		redirectDepthLimit = params.getInt("redirect_depth_limit", REDIRECT_DEPTH_LIMIT);
		saveHtml = params.getBoolean("save_html", SAVE_HTML);
		saveImages = params.getBoolean("save_images", SAVE_IMAGES);
		saveObjects = params.getBoolean("save_objects", SAVE_OBJECTS);
		saveMultimedia = params.getBoolean("save_multimedia", SAVE_MULTIMEDIA);
		saveOthers = params.getBoolean("save_others", SAVE_OTHERS);
		saveFailed = params.getBoolean("save_failed", SAVE_FAILED);
		saveCookies = params.getBoolean("save_cookies", SAVE_COOKIES);
		addReferrer = params.getBoolean("add_referrer", ADD_REFERRER);
		addReferrerCookie = params.getBoolean("add_referrer_cookie", ADD_REFERRER_COOKIE);
		singleJsTimeoutMillis = params.getInt("single_js_timeout", SINGLE_JS_TIMEOUT);
		jsEnable = params.getBoolean("js_enable", JS_ENABLE);
		backgroundJsTimeoutMillis = params.getInt("background_js_timeout", BACKGROUND_JS_TIMEOUT);
		pageTimeoutMillis = params.getInt("page_timeout", PAGE_TIMEOUT);
		processingTimeout = params.getInt("processing_timeout", PROCESSING_TIMEOUT);
		jsRecursionLimit = params.getInt("js_recursion_limit", JS_RECURSION_LIMIT);
		saveJsContext = params.getBoolean("save_js_context", SAVE_JS_CONTEXT);
	}

	public int getProcessExternalLinks() {
		return processExternalLinks;
	}

	public void setProcessExternalLinks(int processExternalLinks) {
		this.processExternalLinks = processExternalLinks;
	}

	public int getLinkLimit() {
		return linkLimit;
	}

	public void setLinkLimit(int linkLimit) {
		this.linkLimit = linkLimit;
	}

	public boolean isSaveHtml() {
		return saveHtml;
	}

	public void setSaveHtml(boolean saveHtml) {
		this.saveHtml = saveHtml;
	}

	public boolean isSaveFailed() {
		return saveFailed;
	}

	public void setSaveFailed(boolean saveFailed) {
		this.saveFailed = saveFailed;
	}

	public boolean isSaveCookies() {
		return saveCookies;
	}

	public void setSaveCookies(boolean saveCookies) {
		this.saveCookies = saveCookies;
	}

	public boolean isAddReferrer() {
		return addReferrer;
	}

	public void setAddReferrer(boolean addReferrer) {
		this.addReferrer = addReferrer;
	}

	public boolean isAddReferrerCookie() {
		return addReferrerCookie;
	}

	public void setAddReferrerCookie(boolean addReferrerCookie) {
		this.addReferrerCookie = addReferrerCookie;
	}

	public boolean isSaveJsContext() {
		return saveJsContext;
	}

	public void setSaveJsContext(boolean saveJsContext) {
		this.saveJsContext = saveJsContext;
	}

	public boolean isSaveImages() {
		return saveImages;
	}

	public void setSaveImages(boolean saveImages) {
		this.saveImages = saveImages;
	}

	public boolean isSaveObjects() {
		return saveObjects;
	}

	public void setSaveObjects(boolean saveObjects) {
		this.saveObjects = saveObjects;
	}

	public boolean isSaveMultimedia() {
		return saveMultimedia;
	}

	public void setSaveMultimedia(boolean saveMultimedia) {
		this.saveMultimedia = saveMultimedia;
	}

	public boolean isSaveOthers() {
		return saveOthers;
	}

	public void setSaveOthers(boolean saveOthers) {
		this.saveOthers = saveOthers;
	}

	public int getPageTimeoutMillis() {
		return pageTimeoutMillis;
	}

	public void setPageTimeoutMillis(int pageTimeoutMillis) {
		this.pageTimeoutMillis = pageTimeoutMillis;
	}

	public int getRedirectTotalLimit() {
		return redirectTotalLimit;
	}

	public void setRedirectTotalLimit(int redirectTotalLimit) {
		this.redirectTotalLimit = redirectTotalLimit;
	}

	public int getRedirectDepthLimit() {
		return redirectDepthLimit;
	}

	public void setRedirectDepthLimit(int redirectDepthLimit) {
		this.redirectDepthLimit = redirectDepthLimit;
	}

	public int getSingleJsTimeoutMillis() {
		return singleJsTimeoutMillis;
	}

	public void setSingleJsTimeoutMillis(int singleJsTimeoutMillis) {
		this.singleJsTimeoutMillis = singleJsTimeoutMillis;
	}

	public int getBackgroundJsTimeoutMillis() {
		return backgroundJsTimeoutMillis;
	}

	public void setBackgroundJsTimeoutMillis(int backgroundJsTimeoutMillis) {
		this.backgroundJsTimeoutMillis = backgroundJsTimeoutMillis;
	}

	public int getJsRecursionLimit() {
		return jsRecursionLimit;
	}

	public void setJsRecursionLimit(int jsRecursionLimit) {
		this.jsRecursionLimit = jsRecursionLimit;
	}

	public int getProcessingTimeout() {
		return processingTimeout;
	}

	public void setProcessingTimeout(int processingTimeout) {
		this.processingTimeout = processingTimeout;
	}

	public boolean getJsEnable() {
		return jsEnable;
	}

	public void setJsEnable(boolean jsEnable) {
		this.jsEnable = jsEnable;
	}
}
