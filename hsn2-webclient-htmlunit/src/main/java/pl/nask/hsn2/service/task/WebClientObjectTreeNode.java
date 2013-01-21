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

package pl.nask.hsn2.service.task;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URIException;
import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.nask.hsn2.RequiredParameterMissingException;
import pl.nask.hsn2.ResourceException;
import pl.nask.hsn2.ServiceConnector;
import pl.nask.hsn2.StorageException;
import pl.nask.hsn2.bus.operations.Attribute;
import pl.nask.hsn2.bus.operations.AttributeType;
import pl.nask.hsn2.service.ServiceData;
import pl.nask.hsn2.service.ServiceParameters;
import pl.nask.hsn2.service.urlfollower.EmbeddedResource;
import pl.nask.hsn2.service.urlfollower.OutgoingLink;
import pl.nask.hsn2.service.urlfollower.PageLinks;
import pl.nask.hsn2.service.urlfollower.PageLinks.LinkType;
import pl.nask.hsn2.service.urlfollower.ScriptInterceptor;
import pl.nask.hsn2.service.urlfollower.ScriptInterceptor.ScriptElement;
import pl.nask.hsn2.service.urlfollower.WebClientObjectType;
import pl.nask.hsn2.service.urlfollower.WebClientOrigin;
import pl.nask.hsn2.service.urlfollower.WebClientWorker;
import pl.nask.hsn2.task.ObjectTreeNode;
import pl.nask.hsn2.utils.Counter;
import pl.nask.hsn2.utils.DataStoreHelper;
import pl.nask.hsn2.utils.WebClientDataStoreHelper;
import pl.nask.hsn2.wrappers.FailedRequestWrapper;
import pl.nask.hsn2.wrappers.FileWrapper;
import pl.nask.hsn2.wrappers.JSContextWrapper;
import pl.nask.hsn2.wrappers.RequestWrapper;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.util.UrlUtils;

public class WebClientObjectTreeNode extends ObjectTreeNode {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebClientObjectTreeNode.class);

	private PageLinks pageLinks;
	private ServiceData inputData;
	private ServiceParameters params;
	
	private WebClientWorker webClientWorker;

	public WebClientObjectTreeNode(long objectDataId) {
		super(objectDataId);
		LOGGER.debug("Created new ObjectTreeNode with dataID: {}",objectDataId);
	}

	public WebClientObjectTreeNode(ObjectTreeNode parent) {
		super(parent);
	}

	@Override
	protected ObjectTreeNode newObjectInstance() {	
		WebClientObjectTreeNode node = new WebClientObjectTreeNode(this);
		LOGGER.debug("Created new ObjectTreeNode: {}",node);
		return node;
	}

	public void webContextInit(ServiceParameters taskParams, ServiceData inputData, WebClientWorker webClientWorker) {
		if (taskParams == null)
			throw new NullPointerException("taskParams");
		if (inputData == null)
			throw new NullPointerException("inputData");
		if (webClientWorker == null)
			throw new NullPointerException("webClientWorker");
		
		this.params = taskParams;
		this.inputData = inputData;
		this.webClientWorker = webClientWorker;
	}

	public PageLinks getPageLinks(ServiceParameters taskParams, ServiceData inputData, WebClientWorker webClientWorker, Counter newObjectsCounter) {
		if (this.webClientWorker == null) {
			this.webClientWorker = webClientWorker;
		}
		if (this.inputData == null) {
			this.inputData = inputData;
		}
		if (params == null) {
			params = taskParams;
		}
		if (pageLinks == null) {
			pageLinks = new PageLinks(inputData.getUrlForProcessing(), taskParams, newObjectsCounter);
		}
		return pageLinks;
	}

	@Override
	protected void prepareForSave(ServiceConnector connector, long jobId) throws StorageException, ResourceException, RequiredParameterMissingException {
		if (pageLinks != null) {
			handleOutgoingLinks();
			handleEmbeddedFiles(connector, jobId);
		}
		if (params != null && params.isSaveJsContext()){
			handleJsContextSaving(connector, jobId);
		}
	}
	
	@Override
	public void flush(ServiceConnector connector, long jobId, List<Long> addedObjects) 
			throws StorageException, ResourceException, RequiredParameterMissingException {
		try{
			if (getParent() == null) { // root node
				// not everything may be mapped to the proper tree nodes, so the root object has to gather all orphans
				flushChildren(connector, jobId, addedObjects);			
				prepareForSave(connector, jobId);
				updateObject(jobId,connector);
				saveNewObjects(connector, jobId, addedObjects);
			} else {
				super.flush(connector, jobId, addedObjects);
			} 
		}  finally{
			cleanNode();
			closeStreams();
		}
	}
	
	@Override
	public void cleanNode() {
		super.cleanNode();
		closeStreams();
		webClientWorker = null;		
	}

	private void handleOutgoingLinks() throws StorageException {
		String url = null;
		for (OutgoingLink link : pageLinks.getOutgoingLinks()) {
			try {
				url = link.getAbsoluteUrl();
				NewWebClientUrlObject newWebClientUrlObject = new NewWebClientUrlObject(
						url,
						WebClientOrigin.LINK.getName(),
						WebClientObjectType.URL.getName(),
						params.isAddReferrer() ? inputData.getUrlForProcessing() : null,
						params.isAddReferrerCookie() ? webClientWorker.getCookiesReferenceIdForCurrentContext() : null);
				addNewObject(newWebClientUrlObject);
			} catch (URIException e) {
				LOGGER.warn("Not an URL!: {}, msg={}", url, e.getMessage());
			}
		}
	}

	private void handleEmbeddedFiles(ServiceConnector connector, long jobId) throws ResourceException, StorageException, RequiredParameterMissingException {
		openStreamsForEmbeddedResources();

		Set<FailedRequestWrapper> failedRequestWrappers = new HashSet<FailedRequestWrapper>();
		Set<FileWrapper> imageWrappers = saveResourceData(connector, jobId, pageLinks.getEmbeddedGroup(LinkType.IMAGE), failedRequestWrappers);
		Set<FileWrapper> objectWrappers = saveResourceData(connector, jobId, pageLinks.getEmbeddedGroup(LinkType.OBJECT), failedRequestWrappers);
		Set<FileWrapper> multimediaWrappers = saveResourceData(connector, jobId, pageLinks.getEmbeddedGroup(LinkType.MULTIMEDIA), failedRequestWrappers);
		Set<FileWrapper> otherWrappers = saveResourceData(connector, jobId, pageLinks.getEmbeddedGroup(LinkType.OTHER), failedRequestWrappers);

		saveResources(connector, jobId, "image_list", imageWrappers);
		saveResources(connector, jobId, "multimedia_list", multimediaWrappers);
		saveResources(connector, jobId, "object_list", objectWrappers);
		saveResources(connector, jobId, "other_list", otherWrappers);

		addNewObjectsForEmbeededResourcesWithServerRedirect();

		if (failedRequestWrappers.size() != 0) {
			long referenceId = WebClientDataStoreHelper.saveFailedRequestsInDataStore(connector, jobId, failedRequestWrappers);
			addRefAttribute("failed_list", referenceId);
		}
	}

	private void addNewObjectsForEmbeededResourcesWithServerRedirect() {
		for (LinkType lt : LinkType.values()) {
			for (EmbeddedResource er : pageLinks.getEmbeddedGroup(lt)) {
				if (er.isServerSideRedirect()) {
					try {
						NewWebClientUrlObject newWebClientUrlObject = new NewWebClientUrlObject(
								er.getAbsoluteUrlBeforeRedirect(),
								WebClientOrigin.SERVER_REDIRECT.getName(),
								WebClientObjectType.URL.getName(),
								params.isAddReferrer() ? inputData.getUrlForProcessing() : null,
								params.isAddReferrerCookie() ? webClientWorker.getCookiesReferenceIdForCurrentContext() : null);
						addNewObject(newWebClientUrlObject);
					} catch (URIException e) {
						// this should never happen
						LOGGER.warn("Not an URL!: {}, msg={}", er.getAbsoluteUrlBeforeRedirect(), e.getMessage());
					}
				}
			}
		}
	}

	private void handleJsContextSaving(ServiceConnector connector, long jobId) throws StorageException {
		Collection<ScriptElement> sources = null;
		if (getParent() != null) {
			Map<String, ScriptElement> sourceByOrigin = webClientWorker.getLaunchedScripts().remove(inputData.getUrlForProcessing());
			if (sourceByOrigin != null) {
				sources = sourceByOrigin.values();
			}
		} else {
			Collection<Map<String, ScriptElement>> allOtherSources = webClientWorker.getLaunchedScripts().values();
			sources = new HashSet<ScriptInterceptor.ScriptElement>();
			for (Map<String, ScriptElement> m: allOtherSources) {
				sources.addAll(m.values());
			}
			webClientWorker.closeJsInterceptor();
			webClientWorker.getLaunchedScripts().clear();
		}
		
		if(sources != null && !sources.isEmpty()){
			List<JSContextWrapper> scriptsWrapper = getJSContextsWrapper(sources);
			saveJSContexts(connector, jobId, scriptsWrapper);
		}
	}
	
	private List<JSContextWrapper> getJSContextsWrapper(Collection<ScriptInterceptor.ScriptElement> scripts) {
		List<JSContextWrapper> ret = new ArrayList<JSContextWrapper>(scripts.size());
		for (ScriptInterceptor.ScriptElement script : scripts) {
			ret.add(new JSContextWrapper(script.getId(), script.getSource(), script.isEval()));
		}
		return ret;
	}
	
	private void saveJSContexts(ServiceConnector connector, long jobId, List<JSContextWrapper> jsContextWrappers) throws StorageException{
		if (jsContextWrappers.size() > 0) {
			long referenceId = WebClientDataStoreHelper.saveJSContextsInDataStore(connector, jobId, jsContextWrappers);

			addRefAttribute("js_context_list", referenceId);
			LOGGER.debug("Adding reference attribute {} with value={} ", "js_context_list", referenceId);
		}
	}

	private Set<FileWrapper> saveResourceData(ServiceConnector connector, long jobId, Set<EmbeddedResource> embeddedGroup,
			Set<FailedRequestWrapper> failedRequests) throws RequiredParameterMissingException, StorageException, ResourceException {
		Set<FileWrapper> objectList = new HashSet<FileWrapper>();
		for (EmbeddedResource embeddedResource : embeddedGroup) {
			List<RequestWrapper> requestList = new ArrayList<RequestWrapper>();
			RequestWrapper requestWrapper = new RequestWrapper(embeddedResource);
			requestList.add(requestWrapper);
			if (embeddedResource.isRequestFailed() || embeddedResource.getResponseCode() != 200) {
				LOGGER.debug("Adding {} to failed list with message {}", embeddedResource, embeddedResource.getFailureMessage());
				FailedRequestWrapper failedRequestWrapper = new FailedRequestWrapper(requestList, embeddedResource.getFailureMessage());
				failedRequests.add(failedRequestWrapper);
			} else {
				String contentType = embeddedResource.getContentType();
				InputStream contentStream = embeddedResource.getContentStream();
				long savedContentId = DataStoreHelper.saveInDataStore(connector, jobId, contentStream);
				FileWrapper fileWrapper = new FileWrapper(requestList, contentType, savedContentId);

				// if (embeddedResource.getLinkType() == LinkType.OBJECT) {
				// processPdfSwfFileContent(embeddedResource.getAbsoluteUrl(),
				// contentType, savedContentId);
				// }
				processPdfSwfFileContent(embeddedResource.getAbsoluteUrl(), contentType, savedContentId);

				objectList.add(fileWrapper);
			}
		}

		return objectList;
	}

	/**
	 * Used when reported URL is not HTML page but some other file.
	 * 
	 * @param url
	 * @param contentType
	 * @param savedContentId
	 * @throws StorageException
	 * @throws ResourceException
	 */
	private void processPdfSwfFileContent(String url, String contentType, long savedContentId) throws StorageException, ResourceException {
    	WebClientObjectType objectType = WebClientObjectType.forMimeType(contentType);
    	if (objectType.isElliglibeForExtract(params)) {
    		try {
    			NewWebClientUrlObject newWebClientUrlObject = new NewWebClientUrlObject(
    					url,
    					WebClientOrigin.EMBEDDED.getName(),
    					objectType.getName(),
    					contentType,
    					params.isAddReferrer() ? inputData.getInputReferrer() : null,
    					params.isAddReferrerCookie() ? inputData.getInputReferrerCookieId() : null,
    					savedContentId);
    			addNewObject(newWebClientUrlObject);
    		} catch (URIException e) {
    			LOGGER.warn("Not an URL!: {}, msg={}", url, e.getMessage());
    		}
    	}
    }

	private void openStreamsForEmbeddedResources() {		
		for (Set<EmbeddedResource> resourcesSet: pageLinks.getEmbeddedResourcesGroups().values()) {
			for(EmbeddedResource resources :resourcesSet){
				processSingleResource(resources);
			}
		}
	}

	private void saveResources(ServiceConnector connector, long jobId, String attributeName, Set<FileWrapper> resourceWrappers) throws StorageException, ResourceException {
		if (resourceWrappers.size() != 0) {
			long referenceId = WebClientDataStoreHelper.saveInDataStore(connector, jobId, resourceWrappers);		
			addRefAttribute(attributeName, referenceId);
		}
	}

	private void processSingleResource(EmbeddedResource resource) {
		
		boolean requestFailed = resource.isRequestFailed();
		if (!requestFailed) {

			String failureReason = resource.getFailureMessage();
			String url = resource.getAbsoluteUrl();
			WebResponse webResponse = null;

			try {
				Page page = webClientWorker.getInsecurePage(url); 
				webResponse = page.getWebResponse();
				int serverRespStatus = webResponse.getStatusCode();
				int resourceRedirLimit = contextHeight();
				while((resourceRedirLimit++ < params.getRedirectDepthLimit() ) &&  serverRespStatus >= HttpStatus.SC_MULTIPLE_CHOICES  && serverRespStatus < HttpStatus.SC_BAD_REQUEST) {
					String redirect = webResponse.getResponseHeaderValue("Location");
					String newUrl = UrlUtils.resolveUrl(url, redirect);
					LOGGER.debug("Resource '{}' redirected to: {}",url,newUrl);
//					NewWebClientUrlObject no = new NewWebClientUrlObject(newUrl, WebClientOrigin.SERVER_REDIRECT.getName(), WebClientObjectType.URL.getName(),0, url, null, null);
//					this.addNewObject(no);
					page = webClientWorker.getInsecurePage(newUrl);
					webResponse = page.getWebResponse();
					serverRespStatus = webResponse.getStatusCode();
				}
				// status message can be used as a failure message
				failureReason = webResponse.getStatusMessage();
			} catch (ConnectTimeoutException e) {
				String msg = "Connection timeout for URL: " + url;
				LOGGER.warn(msg);
				LOGGER.debug(e.getMessage(),e);
				failureReason = getReason(e, msg);
				requestFailed = true;
			} catch (SocketTimeoutException e) {
				String msg = "Socket timeout for URL: " + url;
				LOGGER.warn(msg);
				LOGGER.debug(e.getMessage(),e);
				failureReason = getReason(e, msg);
				requestFailed = true;
			} catch (UnknownHostException e) {
				String msg = "Unknown host for URL: url";
				LOGGER.warn(msg);
				LOGGER.debug(e.getMessage(),e);
				failureReason = getReason(e, msg);
				requestFailed = true;
			} catch (ClientProtocolException e) {
				String msg = "Unsupported protocol. Probably not a resource: " + url;
				LOGGER.error(msg, e);
				failureReason = getReason(e, msg);
				requestFailed = true;
			} catch (IOException e) {
				String msg = "IOException for URL: " + url;
				LOGGER.error(msg, e);
				failureReason = getReason(e, msg);
				requestFailed = true;
			} catch (Exception e) {
				String msg = "Exception for URL: " + url;
				LOGGER.error(msg, e);
				failureReason = getReason(e, msg);
				requestFailed = true;
			} finally {
				webClientWorker.closeAllWindows();
				LOGGER.debug("Trying to close all windows for url: {}",url);
			}
			if(LOGGER.isDebugEnabled()) {		
				LOGGER.debug("Updating resource:{},[{},{},{}]",new Object[] {resource.getAbsoluteUrl(),webResponse !=null ?webResponse.getContentType():"no response",failureReason,requestFailed});
			}
			resource.update(webResponse, failureReason, requestFailed);
		}
	}

	private String getReason(Exception e, String alternativeMsg){
		String msg = e.getMessage();
		if(msg != null){
			return msg;
		}
		else{
			Throwable cause = e.getCause();
			if(cause != null && cause.getMessage() != null){
				return cause.getMessage();
			}
			else{
				return alternativeMsg;
			}
		}
	}

	public ServiceData getServiceData() {
		return inputData;
	}
	
	public void closeStreams() {
		if (pageLinks != null) {
			pageLinks.closeStreams();
		}
	}
	
	public String toString(){
		Attribute originalUrl = getObjectDataBuilder().build().findAttribute("url_original", AttributeType.STRING);
		if (originalUrl != null){
			return originalUrl.getString();
		}
		else{
			return "root";
		}
	}
}
