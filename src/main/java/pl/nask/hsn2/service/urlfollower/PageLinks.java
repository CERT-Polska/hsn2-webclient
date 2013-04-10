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

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.nask.hsn2.service.ServiceParameters;
import pl.nask.hsn2.utils.Counter;

import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;

public class PageLinks {
	private static final Logger LOG = LoggerFactory.getLogger(PageLinks.class);
	
	public enum LinkType {
		OBJECT, MULTIMEDIA, IMAGE, OTHER
	}

	private ServiceParameters params;
	private String baseUrl;
	private boolean isBaseTagIgnored = false;
	private DomElement headElement;
	
	private Map<LinkType, Set<EmbeddedResource>> embeddedResourceGroups = new HashMap<PageLinks.LinkType, Set<EmbeddedResource>>();
	private Set<OutgoingLink> outgoingLinks = new HashSet<OutgoingLink>();
	private Set<FrameLink> redirects = new HashSet<FrameLink>();
	private Counter outgoingLinksCounter;
	
	public PageLinks(String baseUrl, ServiceParameters taskParams, Counter newObjectsCounter) {
		this.baseUrl = baseUrl;
		this.params = taskParams;
		this.outgoingLinksCounter = newObjectsCounter;
		
		if (params.isSaveObjects()) {
			embeddedResourceGroups.put(LinkType.OBJECT, new HashSet<EmbeddedResource>());
		}
		if (params.isSaveMultimedia()) {
			embeddedResourceGroups.put(LinkType.MULTIMEDIA, new HashSet<EmbeddedResource>());
		}
		if (params.isSaveImages()) {
			embeddedResourceGroups.put(LinkType.IMAGE, new HashSet<EmbeddedResource>());
		}
		if (params.isSaveOthers()) {
			embeddedResourceGroups.put(LinkType.OTHER, new HashSet<EmbeddedResource>());
		}
	}

	/**
	 * Adds an embedded 'object' element. Also process 'codebase' and 'archive' attributes.
	 * 
	 * @param element
	 * @param attributeName
	 */
	public void addObject(HtmlElement element, String attributeName) {
		ignoreBaseTag();
		String archive = element.getAttribute("archive");
		String[] archives = null;
		if (archive != DomElement.ATTRIBUTE_NOT_DEFINED) {
			archives = archive.split(" ");
		}
		String codebase = element.getAttribute("codebase");
		if (codebase != DomElement.ATTRIBUTE_NOT_DEFINED && !codebase.endsWith("/")) {
			codebase += "/";
		}
		addEmbedded(LinkType.OBJECT, element, attributeName, codebase, archives);
	}

	public void addMultimedia(HtmlElement element, String attributeName) {
		ignoreBaseTag();
		addEmbedded(LinkType.MULTIMEDIA, element, attributeName);
	}
	
	public void addImage(HtmlElement element, String attributeName) {
		ignoreBaseTag();
		addEmbedded(LinkType.IMAGE, element, attributeName);
	}

	public void addOther(HtmlElement element, String attributeName) {
		if (!element.getTagName().equalsIgnoreCase("html")) {
			// HTML tag do not affect BASE tag
			ignoreBaseTag();
		}
		addEmbedded(LinkType.OTHER, element, attributeName);
	}

	public void addLongdesc(HtmlElement element, String attributeName) {
		ignoreBaseTag();
		processLink(WebClientOrigin.LONGDESC, element, attributeName);
	}		

	public void addAnchor(HtmlElement element, String attributeName) {
		ignoreBaseTag();
		processLink(WebClientOrigin.LINK, element, attributeName);
	}

	private void processLink(WebClientOrigin type, HtmlElement element, String attributeName) {
		if (params.getProcessExternalLinks() == 1 && outgoingLinks.size() < params.getLinkLimit()) {
			String url = element.getAttribute(attributeName);
			if (!empty(url)  && outgoingLinksCounter.countDown()) {
				try {
					outgoingLinks.add(new OutgoingLink(baseUrl, url, type));
				} catch (URISyntaxException e) {
					logUriSyntaxError(url, WebClientOrigin.LINK.getName(), e);
				}				
			} 
		}
	}

	private void logUriSyntaxError(String url, String urlType, URISyntaxException e) {
		LOG.warn("Can't process url (base={}, relative={}, origin={}), error is: {}", new Object[] {baseUrl, url, urlType, e});
		LOG.debug("Can't process url", e);
	}

	private void logDropped(LinkType type, HtmlElement element, String attributeName) {
		LOG.debug("Elememt {} ({}) with attr {} = {} will not be processed by the webclient", 
				new Object[] {element.getNodeName(), type, attributeName, element.getAttribute(attributeName)});		
	}

	private void addEmbeddedArchives(LinkType type, Set<EmbeddedResource> set, String newBaseUrl, String[] archives) {
		if (archives == null) {
			return;
		}
		try {
			for (String arch : archives) {
				EmbeddedResource res = new EmbeddedResource(newBaseUrl, arch, type);
				LOG.debug("Adding embedded resource of type {} : {}", type, res);
				set.add(res);
			}
		} catch (URISyntaxException e) {
			logUriSyntaxError("Archive attribute", type.toString(), e);
		}
	}

	private void addEmbedded(LinkType type, HtmlElement element, String attributeName, String codebase, String[] archives) {
		String newBaseUrl = baseUrl;
		if (!(codebase == null || codebase.isEmpty())) {
			newBaseUrl = codebase;
		}
		Set<EmbeddedResource> set = getSetOf(type);
		if (set != null) {
			String relativeURL = element.getAttribute(attributeName);
			if (!empty(relativeURL)) {
				addEmbeddedArchives(type, set, newBaseUrl, archives);
				try {
					EmbeddedResource res = new EmbeddedResource(newBaseUrl, relativeURL, type);
					LOG.debug("Adding embedded resource of type {} : {}", type, res);
					set.add(res);
				} catch (URISyntaxException e) {
					logUriSyntaxError(relativeURL, type.toString(), e);
				}
			}
		} else {
			logDropped(type, element, attributeName);
		}
	}

	private void addEmbedded(LinkType type, HtmlElement element, String attributeName) {
		addEmbedded(type, element, attributeName, null, null);
	}

	private Set<EmbeddedResource> getSetOf(LinkType type) {
		return embeddedResourceGroups.get(type);
	}

	private boolean empty(String v) {
		return v == null || v.trim().length() == 0;
	}
	
	public Map<LinkType, Set<EmbeddedResource>> getEmbeddedResourcesGroups() {
		
		return embeddedResourceGroups;
	}
	
	public Set<OutgoingLink> getOutgoingLinks() {
		return outgoingLinks;
	}
	
	public Set<FrameLink> getRedirects() {
		return redirects;
	}
	
	public Set<EmbeddedResource> getEmbeddedGroup(LinkType type) {
		Set<EmbeddedResource> res = embeddedResourceGroups.get(type);
		if (res == null) {
			return Collections.emptySet();
		} else {
			return Collections.unmodifiableSet(res);
		}
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public boolean getIsBaseTagIgnored() {
		return isBaseTagIgnored;
	}
	
	public void ignoreBaseTag() {
		isBaseTagIgnored = true;
	}

	public void setHeadElement(DomElement headElement) {
		this.headElement = headElement;
	}

	public boolean isOutsideOfHeadElement(HtmlElement element) {
		boolean result = true;
		if (headElement != null && headElement.isAncestorOf(element)) {
			result = false;
		}
		return result;
	}

	public void closeStreams() {
		LOG.debug("Closing streams");
		for(Set<EmbeddedResource> embeddedResources : embeddedResourceGroups.values()){
			for(EmbeddedResource resource : embeddedResources){
				resource.closeStream();
			}
		}
	}
}
