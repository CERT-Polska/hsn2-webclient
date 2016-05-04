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

package pl.nask.hsn2.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.nask.hsn2.ResourceException;
import pl.nask.hsn2.ServiceConnector;
import pl.nask.hsn2.StorageException;
import pl.nask.hsn2.protobuff.Object.Reference;
import pl.nask.hsn2.protobuff.Resources;
import pl.nask.hsn2.wrappers.CookieWrapper;
import pl.nask.hsn2.wrappers.FailedRequestWrapper;
import pl.nask.hsn2.wrappers.FileWrapper;
import pl.nask.hsn2.wrappers.JSContextWrapper;
import pl.nask.hsn2.wrappers.RequestWrapper;

public final class WebClientDataStoreHelper {
	private static final Logger LOG = LoggerFactory.getLogger(WebClientDataStoreHelper.class);
	
	private WebClientDataStoreHelper() {
		// utility class
	}
	
	public static long saveInDataStore(ServiceConnector connector, long jobId, RequestWrapper requestWrapper) throws StorageException {
        LOG.debug("Adding HTTP Request to data store. " + requestWrapper);
        Resources.Request request = buildRequest(requestWrapper);
        return DataStoreHelper.saveInDataStore(connector, jobId, request.toByteArray());
    }
	
	public static long saveFailedRequestsInDataStore(ServiceConnector connector, long jobId,Set<FailedRequestWrapper> failedRequestWrappers) throws StorageException {
        LOG.debug("Adding failed requests to data store");
        Resources.FailedList.Builder failedListBuilder = Resources.FailedList.newBuilder();
        for (FailedRequestWrapper wrapper : failedRequestWrappers) {
            Resources.FailedRequest failedRequest = Resources.FailedRequest.newBuilder()
                    .addAllRequests(buildRequests(wrapper.getRequestWrappers()))
                    .setReason(wrapper.getFailureReason())
                    .build();
            failedListBuilder.addFailed(failedRequest);
        }
        return DataStoreHelper.saveInDataStore(connector, jobId, failedListBuilder.build().toByteArray());
    }

    public static long saveInDataStore(ServiceConnector connector, long jobId, Set<FileWrapper> fileWrappers) throws StorageException, ResourceException {
        LOG.debug("Adding files to data store");
        Resources.FileList.Builder allFilesListBuilder = Resources.FileList.newBuilder();
        for (FileWrapper wrapper : fileWrappers) {
            long resourceReferenceId = wrapper.getSavedContentId() != null ? wrapper.getSavedContentId() : DataStoreHelper.saveInDataStore(connector, jobId, wrapper.getContentStream());
            Resources.File file = Resources.File.newBuilder()
                    .addAllRequests(buildRequests(wrapper.getRequestWrappers()))
                    .setType(wrapper.getContentType())
                    .setContent(asReferenceBuilder(resourceReferenceId))
                    .build();
            allFilesListBuilder.addFiles(file);
        }
        return DataStoreHelper.saveInDataStore(connector, jobId, allFilesListBuilder.build().toByteArray());
    }

    public static long saveCookiesInDataStore(ServiceConnector connector, long jobId,Set<CookieWrapper> cookieWrappers) throws StorageException {
        LOG.debug("Adding cookie list to data store");
        Resources.CookieList.Builder cookieListBuilder = Resources.CookieList.newBuilder();
        for (CookieWrapper wrapper : cookieWrappers) {
            List<Resources.Cookie.Attribute> attributes = buildCookieAttributes(wrapper);
            Resources.Cookie cookie = Resources.Cookie.newBuilder()
                    .setName(wrapper.getName())
                    .setValue(wrapper.getValue())
                    .addAllAttributes(attributes)
                    .build();
            cookieListBuilder.addCookies(cookie);
        }
        return DataStoreHelper.saveInDataStore(connector, jobId, cookieListBuilder.build().toByteArray());
    }

	public static Set<CookieWrapper> getCookiesFromDataStore(ServiceConnector connector, long jobId, long referenceId) throws StorageException {
		LOG.debug("Getting cookie list from data store");
		Set<CookieWrapper> cookieWrappers = null;
		try {
			try (InputStream responseStream = connector.getDataStoreData(jobId, referenceId)) {
				if (responseStream == null) {
					LOG.debug("Failed to get cookies from data store.");
				} else {
					Resources.CookieList cookieList = Resources.CookieList.parseFrom(responseStream);
					cookieWrappers = getCookiesWrapper(cookieList);
				}
			}
		} catch (IOException e) {
			String msg = "Error getting cookies from data store.";
			LOG.error(msg);
			throw new StorageException(msg, e);
		}
		return cookieWrappers;
	}

    public static long saveJSContextsInDataStore(ServiceConnector connector, long jobId,List<JSContextWrapper> jsContextWrappers) throws StorageException {
        LOG.debug("Adding JS contexts list to data store.");
        Resources.JSContextList.Builder jsContextListBuilder = Resources.JSContextList.newBuilder();
        for (JSContextWrapper wrapper : jsContextWrappers) {
            Resources.JSContext jsContext = Resources.JSContext.newBuilder()
                    .setId(wrapper.getId())
                    .setSource(wrapper.getSource())
                    .setEval(wrapper.isEval())
                    .build();
            jsContextListBuilder.addContexts(jsContext);
        }
        return DataStoreHelper.saveInDataStore(connector, jobId, jsContextListBuilder.build().toByteArray());
    }

    private static List<Resources.Cookie.Attribute> buildCookieAttributes(CookieWrapper cookieWrapper) {
        List<Resources.Cookie.Attribute> attributes = new ArrayList<Resources.Cookie.Attribute>();
        for (Map.Entry<String, String> entry : cookieWrapper.getAttributes().entrySet()) {
            Resources.Cookie.Attribute attribute = Resources.Cookie.Attribute.newBuilder()
            .setName(entry.getKey())
            .setValue(entry.getValue())
            .build();
            attributes.add(attribute);
        }
        return attributes;
    }

    private static Set<CookieWrapper> getCookiesWrapper(Resources.CookieList cookieList) {
        Set<CookieWrapper> cookieWrappers = new HashSet<CookieWrapper>();
        for (Resources.Cookie cookie : cookieList.getCookiesList()) {
            Map<String, String> wrappedAttributes = new HashMap<String, String>();
            for (Resources.Cookie.Attribute attribute : cookie.getAttributesList()) {
                wrappedAttributes.put(attribute.getName(), attribute.getValue());
            }
            cookieWrappers.add(new CookieWrapper(cookie.getName(), cookie.getValue(), wrappedAttributes));
        }
        return cookieWrappers;
    }

    private static Resources.Request buildRequest(RequestWrapper wrapper) {
        Resources.Request.Builder rBuilder = Resources.Request.newBuilder()
                .setRequestUrlOriginal(wrapper.getOriginalUrl())
                .setRequestUrlAbsolute(wrapper.getAbsoluteUrl());

        if (wrapper.getRequestHeader() != null) {
        	rBuilder.setRequestHeader(wrapper.getRequestHeader());
        }
        if (wrapper.getResponseCode() != null) {
        	rBuilder.setResponseCode(wrapper.getResponseCode());
        }
        if (wrapper.getResponseHeader() != null) {
        	rBuilder.setResponseHeader(wrapper.getResponseHeader());
        }
        return rBuilder.build();
    }

    private static List<Resources.Request> buildRequests(List<RequestWrapper> wrappers) {
        List<Resources.Request> ret = new ArrayList<Resources.Request>();
        for (RequestWrapper wrapper : wrappers) {
            Resources.Request request = buildRequest(wrapper);
            ret.add(request);
        }
        return ret;
    }
    
    private static Reference.Builder asReferenceBuilder(long referenceId) {
        return Reference.newBuilder().setKey(referenceId).setStore(DataStoreHelper.DEFAULT_STORE_ID);
    }
}
