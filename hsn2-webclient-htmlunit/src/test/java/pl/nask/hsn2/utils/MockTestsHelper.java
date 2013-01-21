package pl.nask.hsn2.utils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.nask.hsn2.bus.operations.Attribute;
import pl.nask.hsn2.bus.operations.ObjectData;
import pl.nask.hsn2.service.urlfollower.OutgoingLink;
import pl.nask.hsn2.wrappers.CookieWrapper;
import pl.nask.hsn2.wrappers.FailedRequestWrapper;
import pl.nask.hsn2.wrappers.FileWrapper;
import pl.nask.hsn2.wrappers.JSContextWrapper;
import pl.nask.hsn2.wrappers.RequestWrapper;

public class MockTestsHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(MockTestsHelper.class);
	private static final String DELIMITER = "=====================\n";
	public static final String REFERRER = "http://referrer/referrer.html";
	public static final String NON_EXISTANT_HOST_NAME = "noSuchHost-7h4s3az0";
	public static final Set<CookieWrapper> COOKIE_WRAPPERS = new HashSet<CookieWrapper>();

	public static void connectorUpdatesObject(ObjectData data, Set<String> updatedAttributes) {
		if (updatedAttributes == null || !updatedAttributes.isEmpty()) {
			throw new RuntimeException("Before populating with new data, set should be not null and empty.");
		} else {
			StringBuilder sb = new StringBuilder("\n\nUpdating object:\n");
			Set<String> sortedAttributesAndValues = new TreeSet<String>();
			for (Attribute attribute : data.getAttributes()) {
				sortedAttributesAndValues.add(attribute.getName() + "=" + attribute.getValue());
				updatedAttributes.add(attribute.getName());
			}
			for (String attribute : sortedAttributesAndValues) {
				sb.append("--> ").append(attribute).append("\n");
			}
			LOGGER.info(sb.toString());
		}
	}

	public static void connectorUpdatesObject(ObjectData data, Map<String, String> updatedAttributes) {
		if (updatedAttributes == null || !updatedAttributes.isEmpty()) {
			throw new RuntimeException("Before populating with new data, map should be not null and empty.");
		} else {
			StringBuilder sb = new StringBuilder("\n\nUpdating object (MAP):\n");
			Set<String> sortedAttributesAndValues = new TreeSet<String>();
			for (Attribute attribute : data.getAttributes()) {
				sortedAttributesAndValues.add(attribute.getName() + "=" + attribute.getValue());
				String result = updatedAttributes.put(attribute.getName(), attribute.getValue().toString());
				if (result != null) {
					sb.append("==> Warrning! Attribute overwritten: ").append(attribute.getName());
				}
			}
			for (String attribute : sortedAttributesAndValues) {
				sb.append("--> ").append(attribute).append("\n");
			}
			LOGGER.info(sb.toString());
		}
	}

	public static void populateEmbeddedFilesSet(Set<FileWrapper> fileWrappersSet, Set<String> embeddedFiles) {
		if (embeddedFiles == null || !embeddedFiles.isEmpty()) {
			throw new RuntimeException("Before populating with new data, set should be not null and empty.");
		} else {
			embeddedFiles = new HashSet<>();
			StringBuilder sb = new StringBuilder("\n\nEmbedded resources:\n");
			for (FileWrapper fileWrapper : fileWrappersSet) {
				for (RequestWrapper rw : fileWrapper.getRequestWrappers()) {
					embeddedFiles.add(rw.getAbsoluteUrl());
					sb.append("--> AbsUrl=").append(rw.getAbsoluteUrl()).append(", OrgUrl=").append(rw.getOriginalUrl()).append("\n");
				}
			}
			sb.append("\n");
			LOGGER.info(sb.toString());
		}
	}

	public static void populateFailedRequestSet(Set<FailedRequestWrapper> failedRequests, Set<String> failedEmbeddedFiles) {
		if (failedEmbeddedFiles == null || !failedEmbeddedFiles.isEmpty()) {
			throw new RuntimeException("Before populating with new data, set should be not null and empty.");
		} else {
			failedEmbeddedFiles = new HashSet<>();
			StringBuilder sb = new StringBuilder("\n\nFailed embedded resources:\n");
			for (FailedRequestWrapper fileWrapper : failedRequests) {
				for (RequestWrapper rw : fileWrapper.getRequestWrappers()) {
					failedEmbeddedFiles.add(rw.getAbsoluteUrl());
					sb.append("--> AbsUrl=").append(rw.getAbsoluteUrl()).append(", OrgUrl=").append(rw.getOriginalUrl()).append("\n");
				}
			}
			sb.append("\n");
			LOGGER.info(sb.toString());
		}
	}

	public static int checkForJsContext(List<JSContextWrapper> list) {
		int jsSavedScriptCounter = 0;
		StringBuilder sb = new StringBuilder("\n\nJS context save:\n");
		for (JSContextWrapper jsContextWrapper : list) {
			sb.append(DELIMITER);
			sb.append("--> id=").append(jsContextWrapper.getId()).append("; isEval=").append(jsContextWrapper.isEval()).append("\n");
			sb.append("--> source=").append(jsContextWrapper.getSource().trim()).append("\n");
			jsSavedScriptCounter++;
		}
		LOGGER.info(sb.toString());
		return jsSavedScriptCounter;
	}

	public static int saveNewObjectInObjectStore(List<ObjectData> dataList, Set<String> newObjectStoreObjects, int newObjectsInOSCounter) {
		StringBuilder sb = new StringBuilder();
		for (ObjectData objectData : dataList) {
			String url = null;
			String type = null;
			String origin = null;
			int httpCode = -1;
			boolean isActive = true;
			sb.append("\n\nSaving objects:\n");
			Set<Attribute> attrs = objectData.getAttributes();
			Set<String> attrs2 = new TreeSet<String>();
			for (Attribute attribute : attrs) {
				attrs2.add(attribute.getName() + "=" + attribute.getValue());
				switch (attribute.getName()) {
				case "active":
					isActive = attribute.getBool();
					break;
				case "http_code":
					httpCode = attribute.getInteger();
					break;
				case "url_original":
					url = attribute.getString();
					break;
				case "type":
					type = attribute.getString();
					break;
				case "origin":
					origin = attribute.getString();
					break;
				}
			}
			for (String attribute : attrs2) {
				sb.append("--> ").append(attribute).append("\n");
			}
			String newEntry = url + ";" + type + (origin == null ? "" : ";" + origin) + (!isActive ? ";inactive" : "") + (httpCode > 399 ? ";" + httpCode : "");
			newObjectStoreObjects.add(newEntry);
			newObjectsInOSCounter++;
			sb.append("New entry added: ").append(newEntry).append("\n");
			sb.append("New objects created so far: ").append(newObjectsInOSCounter).append("\n");
		}
		LOGGER.info(sb.toString());
		return newObjectsInOSCounter;
	}

	/**
	 * Displays information about HTTP request. Does not gather any data. Only
	 * informational.
	 * 
	 * @param rw
	 */
	public static void saveHttpRequestInDataStore(RequestWrapper rw) {
		String s = "\n\nHTTP request saved:\n";
		s += "--> orig url: " + rw.getOriginalUrl() + "\n";
		s += "--> abs url: " + rw.getAbsoluteUrl() + "\n";
		s += "--> req header: " + rw.getRequestHeader() + "\n";
		s += "--> resp header: " + rw.getResponseHeader() + "\n";
		s += "--> resp code: " + rw.getResponseCode() + "\n";
		LOGGER.info(s);
	}

	public static void saveFailedRequestInDataStore(byte[] data, StringBuilder sb) {
		for (byte b : data) {
			if (b > 31) {
				sb.append((char) b);
			} else {
				sb.append("?");
			}
		}
		LOGGER.info(sb.toString());
	}

	public static boolean isOnOngoingLinksList(Set<OutgoingLink> outgoingLinksSet, String uriToCheck) {
		boolean result = false;
		for (OutgoingLink link : outgoingLinksSet) {
			if (link.getRelativeUrl().equals(uriToCheck)) {
				result = true;
				break;
			}
		}
		return result;
	}

	/**
	 * Displays cookies info. No data is being gathered here. Only
	 * informational.
	 * 
	 * @param set
	 *            Cookies.
	 */
	public static void checkForCookies(Set<CookieWrapper> set) {
		StringBuilder sb = new StringBuilder("\n\nSaving cookies:\n");
		for (CookieWrapper cookieWrapper : set) {
			Map<String, String> attributes = cookieWrapper.getAttributes();
			sb.append("--> Name: " + cookieWrapper.getName() + "\n");
			sb.append("--> Value: " + cookieWrapper.getValue() + "\n");
			sb.append("--> Attributes: " + attributes + "\n");
			sb.append(DELIMITER);
		}
		LOGGER.info(sb.toString());
	}
}
