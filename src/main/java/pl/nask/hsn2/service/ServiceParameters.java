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
	public static final boolean SAVE_MULTIPLE = false; 		private boolean saveMultiple;
	public static final int REDIRECT_DEPTH_LIMIT = 10; 		private int redirectDepthLimit;
	public static final int REDIRECT_TOTAL_LIMIT = 50;  private int redirectTotalLimit;
	public static final int LINK_LIMIT = 100;			private int linkLimit;
	public static final boolean LINK_CLICK_POLICY = true;		private boolean processExternalLinks;
	public static final int JS_RECURSION_LIMIT = 80;	private int jsRecursionLimit;
	public static final boolean JS_ENABLE = true;		private boolean jsEnable;
	public static final String PROFILE = "Firefox 3.6";		private String profile;

	/**
	 * default constructor means, that only default parameters should be used
	 *
	 * @throws ParameterException
	 */
	public ServiceParameters() throws ParameterException {
		this(new ParametersWrapper());
	}

	public ServiceParameters(ParametersWrapper params) throws ParameterException {
		processExternalLinks = params.getBoolean("link_click_policy", LINK_CLICK_POLICY);
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
		saveMultiple = params.getBoolean("save_multiple", SAVE_MULTIPLE);
		addReferrer = params.getBoolean("add_referrer", ADD_REFERRER);
		addReferrerCookie = params.getBoolean("add_referrer_cookie", ADD_REFERRER_COOKIE);
		singleJsTimeoutMillis = params.getInt("single_js_timeout", SINGLE_JS_TIMEOUT);
		jsEnable = params.getBoolean("js_enable", JS_ENABLE);
		backgroundJsTimeoutMillis = params.getInt("background_js_timeout", BACKGROUND_JS_TIMEOUT);
		pageTimeoutMillis = params.getInt("page_timeout", PAGE_TIMEOUT);
		processingTimeout = params.getInt("processing_timeout", PROCESSING_TIMEOUT);
		jsRecursionLimit = params.getInt("js_recursion_limit", JS_RECURSION_LIMIT);
		saveJsContext = params.getBoolean("save_js_context", SAVE_JS_CONTEXT);
		profile = params.get("profile", PROFILE);
	}

	public final boolean getProcessExternalLinks() {
		return processExternalLinks;
	}

	public final void setProcessExternalLinks(boolean processExternalLinks) {
		this.processExternalLinks = processExternalLinks;
	}

	public final int getLinkLimit() {
		return linkLimit;
	}

	public final void setLinkLimit(int linkLimit) {
		this.linkLimit = linkLimit;
	}

	public final boolean isSaveHtml() {
		return saveHtml;
	}

	public final void setSaveHtml(boolean saveHtml) {
		this.saveHtml = saveHtml;
	}

	public final boolean isSaveFailed() {
		return saveFailed;
	}

	public final void setSaveFailed(boolean saveFailed) {
		this.saveFailed = saveFailed;
	}

	public final boolean isSaveCookies() {
		return saveCookies;
	}

	public final void setSaveCookies(boolean saveCookies) {
		this.saveCookies = saveCookies;
	}

	public final boolean isAddReferrer() {
		return addReferrer;
	}

	public final void setAddReferrer(boolean addReferrer) {
		this.addReferrer = addReferrer;
	}

	public final boolean isAddReferrerCookie() {
		return addReferrerCookie;
	}

	public final void setAddReferrerCookie(boolean addReferrerCookie) {
		this.addReferrerCookie = addReferrerCookie;
	}

	public final boolean isSaveJsContext() {
		return saveJsContext;
	}

	public final void setSaveJsContext(boolean saveJsContext) {
		this.saveJsContext = saveJsContext;
	}

	public final boolean isSaveImages() {
		return saveImages;
	}

	public final void setSaveImages(boolean saveImages) {
		this.saveImages = saveImages;
	}

	public final boolean isSaveObjects() {
		return saveObjects;
	}

	public final void setSaveObjects(boolean saveObjects) {
		this.saveObjects = saveObjects;
	}

	public final boolean isSaveMultimedia() {
		return saveMultimedia;
	}

	public final void setSaveMultimedia(boolean saveMultimedia) {
		this.saveMultimedia = saveMultimedia;
	}

	public final boolean isSaveOthers() {
		return saveOthers;
	}

	public final void setSaveOthers(boolean saveOthers) {
		this.saveOthers = saveOthers;
	}

	public final boolean isSaveMultiple() {
		return saveMultiple;
	}

	public final void setSaveMultiple(boolean saveMultiple) {
		this.saveMultiple = saveMultiple;
	}

	public final int getPageTimeoutMillis() {
		return pageTimeoutMillis;
	}

	public final void setPageTimeoutMillis(int pageTimeoutMillis) {
		this.pageTimeoutMillis = pageTimeoutMillis;
	}

	public final int getRedirectTotalLimit() {
		return redirectTotalLimit;
	}

	public final void setRedirectTotalLimit(int redirectTotalLimit) {
		this.redirectTotalLimit = redirectTotalLimit;
	}

	public final int getRedirectDepthLimit() {
		return redirectDepthLimit;
	}

	public final void setRedirectDepthLimit(int redirectDepthLimit) {
		this.redirectDepthLimit = redirectDepthLimit;
	}

	public final int getSingleJsTimeoutMillis() {
		return singleJsTimeoutMillis;
	}

	public final void setSingleJsTimeoutMillis(int singleJsTimeoutMillis) {
		this.singleJsTimeoutMillis = singleJsTimeoutMillis;
	}

	public final int getBackgroundJsTimeoutMillis() {
		return backgroundJsTimeoutMillis;
	}

	public final void setBackgroundJsTimeoutMillis(int backgroundJsTimeoutMillis) {
		this.backgroundJsTimeoutMillis = backgroundJsTimeoutMillis;
	}

	public final int getJsRecursionLimit() {
		return jsRecursionLimit;
	}

	public final void setJsRecursionLimit(int jsRecursionLimit) {
		this.jsRecursionLimit = jsRecursionLimit;
	}

	public final int getProcessingTimeout() {
		return processingTimeout;
	}

	public final void setProcessingTimeout(int processingTimeout) {
		this.processingTimeout = processingTimeout;
	}

	public final boolean getJsEnable() {
		return jsEnable;
	}

	public final void setJsEnable(boolean jsEnable) {
		this.jsEnable = jsEnable;
	}

	public final String getProfile() {
		return profile;
	}

	public final void setProfile(String profile) {
		this.profile = profile;
	}
}
