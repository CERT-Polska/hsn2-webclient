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

package pl.nask.hsn2.service.task;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.nask.hsn2.ContextSizeLimitExceededException;
import pl.nask.hsn2.NewUrlObject;
import pl.nask.hsn2.ParameterException;
import pl.nask.hsn2.ServiceConnector;
import pl.nask.hsn2.StorageException;
import pl.nask.hsn2.TaskContext;
import pl.nask.hsn2.service.ServiceData;
import pl.nask.hsn2.service.ServiceParameters;
import pl.nask.hsn2.service.urlfollower.PageLinks;
import pl.nask.hsn2.service.urlfollower.ScriptInterceptor.ScriptElement;
import pl.nask.hsn2.service.urlfollower.WebClientWorker;
import pl.nask.hsn2.task.ObjectTreeNode;
import pl.nask.hsn2.utils.Counter;
import pl.nask.hsn2.utils.WebClientDataStoreHelper;
import pl.nask.hsn2.wrappers.CookieWrapper;
import pl.nask.hsn2.wrappers.RequestWrapper;

public class WebClientTaskContext extends TaskContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebClientTaskContext.class);
	private ServiceParameters taskParams;
	private ServiceData inputData;
	private WebClientWorker webClientWorker;
	private Counter newObjectsCounter = new Counter(Integer.MAX_VALUE);
	private Map<String, WebClientObjectTreeNode> urlNodeIndex = new HashMap<String, WebClientObjectTreeNode>();
	private Long cookiesReferenceId;

	public WebClientTaskContext(long jobId, int reqId, long objectDataId, ServiceConnector connector) {
        super(jobId, reqId, objectDataId, connector, new WebClientObjectTreeNode(objectDataId));
    }

    public final long saveInDataStore(RequestWrapper requestWrapper) throws StorageException, ParameterException {
        return WebClientDataStoreHelper.saveInDataStore(connector, jobId, requestWrapper);
    }

    public final Long getCookiesReferenceId() {
    	return cookiesReferenceId;
    }

    public final void saveCookiesInDataStore(Set<CookieWrapper> cookieWrappers) throws StorageException {
    	cookiesReferenceId = WebClientDataStoreHelper.saveCookiesInDataStore(connector, jobId, cookieWrappers);
    }

    public final Set<CookieWrapper> getCookiesFromDataStore(long referenceId) throws StorageException {
    	return WebClientDataStoreHelper.getCookiesFromDataStore(connector, jobId, referenceId);
    }

	public final void setServiceParams(ServiceParameters taskParams) {
		this.taskParams = taskParams;
		newObjectsCounter = new Counter(taskParams.getLinkLimit());
		// tree includes root object, which height and size are 1, the limits refer to it's children
		treeHeightLimit = taskParams.getRedirectDepthLimit() + 1;
		treeSizeLimit = taskParams.getRedirectTotalLimit() + 1;
	}

	public final void setServiceData(ServiceData data) {
		inputData = data;
	}
	public final ServiceData getServiceData() {
		return inputData;
	}

	public final PageLinks getPageLinks() {
		return ((WebClientObjectTreeNode) getCurrentContext()).getPageLinks(taskParams, inputData, webClientWorker, newObjectsCounter);
	}

	@Override
	public final void newObject(NewUrlObject newObject) throws StorageException {
		if (newObjectsCounter.countDown()) {
			super.newObject(newObject);
		} else {
			LOGGER.debug("OutgoingLinksLimit reached, skipping {}", newObject);
		}
	}

	public final void setWebClientWorker(WebClientWorker webClientWorker) {
		this.webClientWorker = webClientWorker;
	}

	public final Long getInputDataInputReferrerCookieId() {
		return inputData.getInputReferrerCookieId();
	}

	public final ServiceData getCurrentContextServiceData() {
		return ((WebClientObjectTreeNode) getCurrentContext()).getServiceData();
	}

	public final void webContextInit(String newUrlForProcessing, String newReferrer, Long newReferrerCookieId) {
		ServiceData newInputData = inputData.getServiceDataCopyForNewSubcontext(newUrlForProcessing, newReferrer, newReferrerCookieId);
		WebClientObjectTreeNode currentContext = (WebClientObjectTreeNode) getCurrentContext();
		currentContext.webContextInit(taskParams, newInputData, webClientWorker);
		urlNodeIndex.put(newUrlForProcessing, currentContext);
	}

	public final ObjectTreeNode getContextByUrl(String url){
		return urlNodeIndex.get(url);
	}

	public final Map<String, Map<String, ScriptElement>> getLaunchedScriptsByOrigin(){
		return webClientWorker.getLaunchedScripts();
	}
	@Override
	public final void openSubContext() throws ContextSizeLimitExceededException {
		LOGGER.debug("Opening SubContext:{}",super.getCurrentContext().getParent());
		super.openSubContext();
	}
}
