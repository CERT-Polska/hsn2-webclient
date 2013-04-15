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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.nask.hsn2.ContextSizeLimitExceeded;
import pl.nask.hsn2.ParameterException;
import pl.nask.hsn2.RequiredParameterMissingException;
import pl.nask.hsn2.ResourceException;
import pl.nask.hsn2.StorageException;
import pl.nask.hsn2.bus.api.TimeoutException;
import pl.nask.hsn2.service.ServiceData;
import pl.nask.hsn2.service.ServiceParameters;
import pl.nask.hsn2.service.task.NewWebClientUrlObject;
import pl.nask.hsn2.service.task.WebClientTaskContext;
import pl.nask.hsn2.service.urlfollower.ScriptInterceptor.ScriptElement;
import pl.nask.hsn2.wrappers.CookieWrapper;
import pl.nask.hsn2.wrappers.RequestWrapper;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.DefaultCredentialsProvider;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.HttpWebConnection;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlFrame;
import com.gargoylesoftware.htmlunit.html.HtmlInlineFrame;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.gargoylesoftware.htmlunit.util.UrlUtils;

public class WebClientWorker implements Runnable {
	private static final int ONE_SECOND_IN_MILISECONDS = 1000;
	private static final String HTML_STRING = "html";
	private static final Logger LOGGER = LoggerFactory.getLogger(WebClientWorker.class);
	private static final String URL_ORIGINAL_STRING = "url_original";
	private static final String HREF_STRING = "href";
	private static final String SRC_STRING = "src";

	private final ScriptInterceptor scriptInterceptor;
	private WebClient wc;
	private final CountDownLatch latch;
	private final HtmlUnitFollower workerDispatcher;
	private ServiceParameters taskParams;
	private Map<Page,ProcessedPage> previousTopPageMap = new HashMap<Page, ProcessedPage>();
	private Map<Page,ProcessedPage> previousFramePageMap = new HashMap<Page, ProcessedPage>();
	private WebClientTaskContext ctx;
	private volatile boolean interruptProcessing;

	public WebClientWorker(HtmlUnitFollower dispatcher, CountDownLatch l, WebClientTaskContext ctx, ServiceParameters taskParams) {
		if (taskParams == null) {
			throw new IllegalArgumentException("ServiceParameters cannot be null");
		}
		if (l == null) {
			throw new IllegalArgumentException("CountDownLatch cannot be null");
		}
		if (dispatcher == null) {
			throw new IllegalArgumentException("HtmlUnitFollower cannot be null");
		}
		this.latch = l;
		this.workerDispatcher = dispatcher;
		this.scriptInterceptor = new ScriptInterceptor(taskParams);
		this.taskParams = taskParams;
		this.ctx = ctx;
	}

	private void initializeWebClient() {
		String proxy = null;
		ProxyParamsWrapper proxyParams = null;
		if ( ctx != null && ctx.getCurrentContextServiceData() != null) {
			proxy = ctx.getCurrentContextServiceData().getProxyUri();
		}
		if ( proxy == null || proxy.trim().isEmpty()) {
			wc = new WebClient(getBrowserVersion());
		} else {
			proxyParams = new ProxyParamsWrapper(proxy);
			if ( proxyParams.isProxy()) {
				wc = new WebClient(getBrowserVersion(), proxyParams.getHost(), proxyParams.getPort());
				if (proxyParams.isSocksProxy()) {
					wc.getProxyConfig().setSocksProxy(true);
				}
				if ( proxyParams.hasUserCredentials()) {
					DefaultCredentialsProvider dc = (DefaultCredentialsProvider) wc.getCredentialsProvider();
					dc.addCredentials(proxyParams.getUserName(), proxyParams.getUserPswd(), proxyParams.getHost(), proxyParams.getPort(), null);
				}
			} else {
				LOGGER.warn("Incorrect proxy params: {}.proxy disabled.",proxy);
				wc = new WebClient();
			}
		}

		// http errors and script errors are not considered an error here
		wc.setRedirectEnabled(false);

		// don't process activeX!
		wc.setActiveXNative(false);

		wc.setJavaScriptEnabled(taskParams.getJsEnable());

		wc.setHomePage("http://unknown.unknown/");
		wc.setTimeout(taskParams.getPageTimeoutMillis());
		wc.setJavaScriptTimeout(taskParams.getSingleJsTimeoutMillis());
		wc.setThrowExceptionOnFailingStatusCode(false);

		// disable script errors
		wc.setThrowExceptionOnScriptError(false);

		wc.getJavaScriptEngine().getContextFactory().setDebugger(scriptInterceptor);
		wc.setRefreshHandler(new MetaRedirectHandler(taskParams.getPageTimeoutMillis(), taskParams.getRedirectDepthLimit()));
		wc.setJavaScriptErrorListener(new JsScriptErrorListener());
		wc.addWebWindowListener(new WebWindowListenerImpl(previousTopPageMap, previousFramePageMap));	
		LOGGER.info("Initialized WebClientWorker with options: [JsEnabled={}], [ActiveXNative={}], [processing_timeout={}], [page_timeout={}] , [proxy:{}] ",
				new Object[] {wc.isJavaScriptEnabled(),wc.isActiveXNative(),taskParams.getProcessingTimeout(),taskParams.getPageTimeoutMillis(),proxyParams});
		LOGGER.info("\n\n############################\nbrowser ver = {}\n\n", wc.getBrowserVersion().getNickname());
	}

	/**
	 * Returns browser version according to browser profile set in service parameters.<br>
	 * <br>
	 * Below you can find list of currently supported browsers.
	 * <ul>
	 * <li>Internet Explorer 6</li>
	 * <li>Internet Explorer 7</li>
	 * <li>Internet Explorer 8</li>
	 * <li>Firefox 3</li>
	 * <li>Firefox 3.6 - default, if browser name in parameter does not match any of listed names</li>
	 * <li>Firefox 10</li>
	 * <li>Chrome 16</li>
	 * </ul>
	 * 
	 * @return Browser version.
	 */
	@SuppressWarnings("deprecation")
	private BrowserVersion getBrowserVersion() {
		String profileName = taskParams.getProfile();
		switch (profileName) {
		case "Internet Explorer 6":
			return BrowserVersion.INTERNET_EXPLORER_6;
		case "Internet Explorer 7":
			return BrowserVersion.INTERNET_EXPLORER_7;
		case "Internet Explorer 8":
			return BrowserVersion.INTERNET_EXPLORER_8;
		case "Firefox 3":
			return BrowserVersion.FIREFOX_3;
		case "Firefox 10":
			return BrowserVersion.FIREFOX_10;
		case "Chrome 16":
			return BrowserVersion.CHROME_16;
		default:
			LOGGER.warn("Browser profile '{}' not supported. Using default Firefox 3.6 instead.", profileName);
			return BrowserVersion.FIREFOX_3_6;
		}
	}

	@Override
	public final void run() {
		String workerUrl = workerDispatcher.getUrlForProcessing();
		try {
			initializeWebClient();
			processTheUrl(workerUrl);
		} catch (ConnectTimeoutException e) {
			LOGGER.warn("Connection timeout for URL '{}'", workerUrl);
			LOGGER.debug(e.getMessage(), e);
			workerDispatcher.requestFailed(e);
		} catch (org.apache.http.conn.ConnectTimeoutException e) {
			LOGGER.warn("Connection timeout: {}", e.getMessage());
			LOGGER.debug("Connection timeout stacktrace: {}", e);
			workerDispatcher.requestFailed(e);
		} catch (SocketTimeoutException e) {
			LOGGER.warn("Socket timeout for URL '{}'", workerUrl);
			LOGGER.debug(e.getMessage(),e);
			workerDispatcher.requestFailed(e);
		} catch (UnknownHostException e) {
			LOGGER.warn("Unknown host: {}",e.getMessage());
			LOGGER.debug("Unknown host stacktrace: {}", e);
			workerDispatcher.requestFailed("Unknown host: " + e.getMessage());
		} catch (IOException e) {
			LOGGER.warn("IOException: {}", e.getMessage());
			LOGGER.debug("IOException for URL '{}' with stacktrace: {}", workerUrl, e);
			workerDispatcher.requestFailed(e);
		}  catch (TimeoutException e) {
			LOGGER.warn(e.getMessage());
			LOGGER.debug(e.getMessage(),e);
			workerDispatcher.requestFailed(e);
		} catch (Exception e) {
			LOGGER.error("Exception for URL '{}'", workerUrl, e);
			workerDispatcher.requestFailed(e);
		} finally {
			closeAllWindows();
			latch.countDown();
		}
	}

	public final void stopProcessing() {
		interruptProcessing = true;
		LOGGER.debug("Setting: 'stop processing'");
		stopJavaScripts();
	}

	private void processTheUrl(String url) throws IOException, ParameterException, ResourceException,
			StorageException, BreakingChainException, ExecutionException, TimeoutException {
		if ( interruptProcessing) {
			LOGGER.debug("Time limit exceeded. {} won't be processed",url);

			// it's thrown in getInsecurePagesChain() so might be omitted here
			throw new TimeoutException("Timeout, stopping processing:"+ url);
		}
		LOGGER.debug("Gathering page {}", url);
		long startTime = System.currentTimeMillis();
		ProcessedPage rootPage = null;
		try {
			rootPage = getInsecurePagesChain(url);
			workerDispatcher.setPage(rootPage);
		} catch (IOException e) {
			LOGGER.warn("Exception while gathering page: '{}'", url);
			LOGGER.debug("Exception while gathering page (stacktrace):", e);
			throw e;
		}
		processPage(rootPage);

		long pageProcessedTime = System.currentTimeMillis();
		LOGGER.debug("Processing of {} took {} ms. ", url, (pageProcessedTime - startTime));
	}

	private void processPage(ProcessedPage processedPage) throws FailingHttpStatusCodeException, MalformedURLException, IOException,
			ParameterException, ResourceException, StorageException {
		try {			
			int i = wc.waitForBackgroundJavaScript(taskParams.getBackgroundJsTimeoutMillis());
			if (i > 0) {
				LOGGER.warn("There are still {} javascripts runnig in background", i);
			}
			restartJavaScript();
			
			long pageGatheredTime = System.currentTimeMillis();

			if (processedPage.getClientSideRedirectPage() != null) {
				processClientRedirectSubPage(processedPage.getClientSideRedirectPage());
				handlePage(processedPage);
			} else if (processedPage.getServerSideRedirectLocation() != null) {
				processServerRedirectSubPage(processedPage);
			} else {
				handlePage(processedPage);
			}

			long pageProcessedTime = System.currentTimeMillis();
			LOGGER.debug("Inspecting of {} took {} ms. ", processedPage.getRequestedUrl(), (pageProcessedTime - pageGatheredTime));
		} catch (BreakingChainException e) {
			LOGGER.error("Error when processing " + processedPage.getActualUrl() + "(requested: " + processedPage.getRequestedUrl() + "). Some data may be lost!",e);
		} finally {
			if(processedPage != null) {
				addRequiredAttributesToCurrentContext(processedPage);
				processedPage.cleanPage();
			}
		}
	}
	
	public void stopJavaScripts() {
		wc.setJavaScriptEnabled(false);
		wc.getJavaScriptEngine().shutdownJavaScriptExecutor();
		JsScriptDebugFrame.resetCounter();
		LOGGER.debug("JavaScript was stopped.");
	}
	
	private void restartJavaScript(){
		stopJavaScripts();
		wc.setJavaScriptEnabled(true);
		LOGGER.debug("JavaScript was restarted.");
	}

	private void handlePage(ProcessedPage processedPage) throws IOException, ParameterException, ResourceException, StorageException {
		if (processedPage.isHtml()) {
			LOGGER.debug("Got HTML page, processing. (url={})", processedPage.getRequestedUrl());
			handleHtmlPage((HtmlPage) processedPage.getPage());
		} else if (processedPage.getPage() instanceof TextPage) {
			handleTextPage();
		} else {
			LOGGER.warn("Unsupported page type ({}) wile parsing URL ({})", new Object[] {
					processedPage.getPage().getWebResponse().getContentType(),
					processedPage.getPage().getWebResponse().getWebRequest().getUrl().toExternalForm() });
		}
	}

	private void processFramesSubPage(ProcessedPage subPage, String subPageUrl, WebClientOrigin origin) throws IOException,
			ParameterException, ResourceException, StorageException {
		boolean openSubContextFailed = false;
		try {
			String oldBaseUrl = getPageLinksForCurrentContext().getBaseUrl();
			String newSubPageUrl = null;

			if (subPage == null) {
				// HtmlUnit shows about:blank frame page when it can't
				// follow it (i.e. when it is ftp:// or another protocol).
				if (!subPageUrl.isEmpty()) {
					// Frame has not been followed, so we have to use url from
					// source, not from page.
					// In most cases that means protocol was not supported.
					try {
						Link frameUrlFromSource = new Link(oldBaseUrl, subPageUrl);
						newSubPageUrl = frameUrlFromSource.getAbsoluteUrl();
					} catch (URISyntaxException e) {
						// This is unlikely to happen.
						LOGGER.debug("Can't create Link for subpage: {}", e.getMessage());
						LOGGER.debug("", e);
						newSubPageUrl = "about:blank";
					}
				} else {
					newSubPageUrl = "about:blank";
				}
				prepareSubPage(newSubPageUrl, origin);
				addRequiredAttributesToCurrentContext(null);
			} else {
				newSubPageUrl = subPage.getActualUrl().toExternalForm();
				prepareSubPage(newSubPageUrl, origin);
				processPage(subPage);
			}
		} catch (ContextSizeLimitExceeded e) {
			LOGGER.debug("Couldn't open subcontext: size limit reached: {}", e.getMessage());
			openSubContextFailed = true;
		} catch (URIException e) {
			// Protocol not supported, but object has to be created and url_original set.
			LOGGER.debug("Protocol not supported (not HTTP/HTTPS) for iframe:" + e.getMessage());
			ctx.addAttribute(URL_ORIGINAL_STRING, e.getMessage());
		} finally {
			if (!openSubContextFailed) {
				ctx.closeSubContext();
			}
		}
	}

	private void processClientRedirectSubPage(ProcessedPage subPage) throws IOException, ParameterException, ResourceException,
			StorageException {
		boolean openSubContextFailed = false;
		try {
			prepareSubPage(subPage.getActualUrl().toExternalForm(), WebClientOrigin.CLIENT_REDIRECT);
			processPage(subPage);
		} catch (ContextSizeLimitExceeded e) {
			LOGGER.debug("Couldn't open subcontext: size limit reached: {}", e.getMessage());
			openSubContextFailed = true;
		} catch (URIException e) {
			// Protocol not supported, but object has to be created and url_original set.
			LOGGER.debug("Protocol not supported (not HTTP/HTTPS) for iframe:" + e.getMessage());
			ctx.addAttribute(URL_ORIGINAL_STRING, e.getMessage());
		} finally {
			if (!openSubContextFailed) {
				ctx.closeSubContext();
			}
		}

	}

	private void processServerRedirectSubPage(ProcessedPage processedPage) throws IOException, ParameterException, ResourceException,
			StorageException, BreakingChainException {
		boolean openSubContextFailed = false;
		try {
			prepareSubPage(processedPage.getServerSideRedirectLocation(), WebClientOrigin.SERVER_REDIRECT);

			ProcessedPage newSubPage = getInsecurePagesChain(processedPage);
			processPage(newSubPage);
		} catch (ContextSizeLimitExceeded e) {
			LOGGER.debug("Couldn't open subcontext: size limit reached: {}", e.getMessage());
			openSubContextFailed = true;
		} catch (URIException e) {
			// Protocol not supported, but object has to be created and url_original set.
			LOGGER.debug("Protocol not supported (not HTTP/HTTPS) for iframe:" + e.getMessage());
			ctx.addAttribute(URL_ORIGINAL_STRING, e.getMessage());
		} catch (ExecutionException e) {
			openSubContextFailed = true;
		} catch (TimeoutException e) {
			LOGGER.debug("Time limit exceeded: {}",processedPage.getActualUrl());
			openSubContextFailed = true;		
		} finally {
			if (!openSubContextFailed) {
				ctx.closeSubContext();
			}
		}
	}

	private ProcessedPage getInsecurePagesChain(String url) throws IOException, BreakingChainException, ExecutionException,
			TimeoutException {
		Page resultingPage = null;
		resultingPage = getInsecurePage(url);
		
		ProcessedPage chain = previousTopPageMap.get(resultingPage);
		if(chain == null){
				throw new BreakingChainException(resultingPage);
		}
		return chain;
	}

	private ProcessedPage getInsecurePagesChain(final ProcessedPage processedPage) throws IOException, BreakingChainException,
			ExecutionException, TimeoutException {
		final WebRequest req = insecurePagesChaingInitialization(processedPage);
		ExecutorService ex = Executors.newSingleThreadExecutor();
		Future<Page> f = ex.submit(new Callable<Page>() {
			@Override
			public Page call() throws IOException {
				return wc.getPage(processedPage.getPage().getEnclosingWindow(), req);
			}
		});
		Page p = null;
		try {
			if (!interruptProcessing){
				if (taskParams.getPageTimeoutMillis() <= 0) {
					p = f.get();
				}
				else {
					p = f.get(taskParams.getPageTimeoutMillis(), TimeUnit.MILLISECONDS);
				}
			}
		} catch (InterruptedException e) {
			LOGGER.warn("Gathering {} interrupted", req.getUrl());
			Thread.currentThread().interrupt();
		} catch (java.util.concurrent.TimeoutException e) {
			throw new TimeoutException("Timeout when gathering:" + req.getUrl(), e);
		} finally {
			if (f != null) {
				f.cancel(true);
			}
			closeExecutorWithJSDisabled(ex);
		}
		return insecurePagesChainPostprocessing(processedPage, p);
	}

	private void closeExecutorWithJSDisabled(ExecutorService ex){
		wc.setJavaScriptEnabled(false);
		try {
			ex.awaitTermination(500, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			//ignore
		}
		wc.setJavaScriptEnabled(true);
	}
	
	private ProcessedPage insecurePagesChainPostprocessing(final ProcessedPage processedPage, Page p) throws BreakingChainException {
		ProcessedPage chain = null;
		if (processedPage.isFromFrame()) {
			chain = previousFramePageMap.get(p);
		} else {
			chain = previousTopPageMap.get(p);
		}
		if (chain == null) {
			throw new BreakingChainException("Page: " + p + " dosn't have chain!\nTopMapKey: " + previousTopPageMap.keySet()
					+ "\nFrameMapKey:" + previousFramePageMap.keySet());
		}
		return chain;
	}

	private WebRequest insecurePagesChaingInitialization(final ProcessedPage processedPage) throws TimeoutException, MalformedURLException {
		if (interruptProcessing) {
			throw new TimeoutException("Overall time limit exceeded url:" + processedPage.getOriginalUrl());
		}
		try {
			wc.setUseInsecureSSL(true);
		} catch (GeneralSecurityException e) {
			LOGGER.error(e.getMessage(), e);
		}
		final WebRequest req = new WebRequest(UrlUtils.toUrlUnsafe(processedPage.getServerSideRedirectLocation()));
		req.setAdditionalHeader("Accept-Encoding", "");
		return req;
	}

	private void prepareSubPage(String subPageUrl, WebClientOrigin origin) throws ContextSizeLimitExceeded, URIException {
		ServiceData curCtxServiceData = ctx.getCurrentContextServiceData();
		String newSubpageReferrer = curCtxServiceData.getUrlForProcessing();
		Long newSubpageReferrerCookiesId = ctx.getCookiesReferenceId();
		ctx.openSubContext();
		ctx.addAttribute("type", "url");
		ctx.addAttribute("origin", origin.getName());
		ctx.addAttribute(URL_ORIGINAL_STRING, subPageUrl);
		ctx.webContextInit(subPageUrl, newSubpageReferrer, newSubpageReferrerCookiesId);
		validateSupportedProtocols(subPageUrl);
	}

	private void handleHtmlPage(HtmlPage page) throws IOException, ParameterException, ResourceException, StorageException {
		inspectHtmlPage(page);
	}

	private void handleTextPage() {
		ctx.addAttribute(HTML_STRING, false);
	}

	private void inspectHtmlPage(HtmlPage htmlPage) throws IOException, ParameterException, ResourceException, StorageException {
		getPageLinksForCurrentContext().setBaseUrl(htmlPage.getUrl().toExternalForm());
		for (HtmlElement element : htmlPage.getHtmlElementDescendants()) {
			inspect(element);
		}
	}

	private void inspect(HtmlElement element) throws IOException, ParameterException, ResourceException, StorageException {
		String tagName = element.getTagName().toLowerCase();
		if (interruptProcessing) {
			LOGGER.debug("Element [{}] won't be processed (timeout)", tagName);
		} else if ("head".equals(tagName)) {
			// Process HEAD tag.
			getPageLinksForCurrentContext().setHeadElement(element);
		} else if ("base".equals(tagName) && !getPageLinksForCurrentContext().getIsBaseTagIgnored()) {
			// Process BASE tag.
			processBaseTag(element);
		} else if ("applet".equals(tagName)) {
			// Process APPLET tag.
			processEmbeddedObjectFile(element, "code");
			processEmbeddedObjectFile(element, "classid");
		} else if ("audio".equals(tagName)) {
			// Process AUDIO tag.
			processEmbeddedMultimediaFile(element, SRC_STRING);
		} else if ("body".equals(tagName)) {
			// Process BODY tag.
			processEmbeddedImageFile(element, "background");
		} else if ("command".equals(tagName)) {
			// Process COMMAND tag.
			processEmbeddedImageFile(element, "icon");
		} else if ("embed".equals(tagName)) {
			// Process EMBED tag.
			processEmbeddedObjectFile(element, SRC_STRING);
		} else if (HTML_STRING.equals(tagName)) {
			// Process HTML tag. BASE and CODEBASE tags can't influence HTML MANIFEST.
			processEmbeddedOtherFile(element, "manifest");
		} else if ("img".equals(tagName)) {
			// Process IMG tag.
			processEmbeddedImageFile(element, SRC_STRING);
			getPageLinksForCurrentContext().addLongdesc(element, "longdesc");
		} else if ("input".equals(tagName)) {
			// Process INPUT tag.
			if ("image".equalsIgnoreCase(element.getAttribute("type"))) {
				processEmbeddedImageFile(element, SRC_STRING);
			}
		} else if ("link".equals(tagName)) {
			// Process LINK tag.
			processLinkTag(element);
		} else if ("object".equals(tagName)) {
			// Process OBJECT tag.
			processObjectTag(element);
		} else if ("video".equals(tagName)) {
			// Process VIDEO tag.
			processEmbeddedImageFile(element, "poster");
			processEmbeddedMultimediaFile(element, SRC_STRING);
		} else if ("script".equals(tagName)) {
			// Process SCRIPT tag.
			processEmbeddedOtherFile(element, SRC_STRING);
		} else if ("source".equals(tagName)) {
			// Process SOURCE tag.
			processEmbeddedMultimediaFile(element, SRC_STRING);
		} else if ("a".equals(tagName)) {
			// Process A tag. (This is not embedded file but ongoing link.)
			getPageLinksForCurrentContext().addAnchor(element, HREF_STRING);
		} else if ("area".equals(tagName)) {
			// Process AREA tag. (This is not embedded file but ongoing link.)
			getPageLinksForCurrentContext().addAnchor(element, HREF_STRING);
		} else if ("frame".equals(tagName)) {
			// Process FRAME tag.
			getPageLinksForCurrentContext().ignoreBaseTag();
			processFramesSubPage(previousFramePageMap.get(((HtmlFrame) element).getEnclosedPage()), element.getAttribute(SRC_STRING), WebClientOrigin.FRAME);
		} else if ("iframe".equals(tagName)) {
			// Process IFRAME tag.
			getPageLinksForCurrentContext().ignoreBaseTag();
			processFramesSubPage(previousFramePageMap.get(((HtmlInlineFrame) element).getEnclosedPage()), element.getAttribute(SRC_STRING),
					WebClientOrigin.IFRAME);
		}
	}

	private void processObjectTag(HtmlElement element) throws IOException, ParameterException, ResourceException, StorageException {
		processEmbeddedObjectFile(element, "data");
		String classIdAttribute = element.getAttribute("classid").toLowerCase();
		boolean classidIsUrl = classIdAttribute.startsWith("http:") || classIdAttribute.startsWith("https:");
		if (classidIsUrl) {
			processEmbeddedObjectFile(element, "classid");
		}
	}

	private void processLinkTag(HtmlElement element) throws IOException, ParameterException, ResourceException, StorageException {
		String rel = element.getAttribute("rel");
		if ("stylesheet".equalsIgnoreCase(rel)) {
			processEmbeddedOtherFile(element, HREF_STRING);
		} else if ("icon".equalsIgnoreCase(rel) || "shortcut icon".equalsIgnoreCase(rel)) {
			processEmbeddedImageFile(element, HREF_STRING);
		}
	}

	private void processBaseTag(HtmlElement element) {
		if (getPageLinksForCurrentContext().isOutsideOfHeadElement(element)) {
			// BASE element should be inside HEAD, but it isn't.
			getPageLinksForCurrentContext().ignoreBaseTag();
			return;
		}
		String base = element.getAttribute(HREF_STRING);
		if (properUrl(base)) {
			if (!base.endsWith("/")) {
				base += "/";
			}
			getPageLinksForCurrentContext().setBaseUrl(base);
		}
	}

	private void processEmbeddedObjectFile(HtmlElement element, String attributeName) throws IOException, ParameterException,
			ResourceException, StorageException {
		// Ignore future BASE tags.
		getPageLinksForCurrentContext().ignoreBaseTag();

		if (taskParams.isSaveObjects()) {
			// Check if provided attribute exists.
			String attributeNameChecked = element.getAttribute(attributeName);
			if (attributeNameChecked == DomElement.ATTRIBUTE_NOT_DEFINED) {
				return;
			}

			// Check for ARCHIVE attribute.
			String[] archives = null;
			String archiveAttribute = element.getAttribute("archive");
			if (archiveAttribute != DomElement.ATTRIBUTE_NOT_DEFINED) {
				archives = archiveAttribute.split(" ");
			}

			// Check for CODEBASE attribute.
			String codebase = element.getAttribute("codebase");
			if (codebase != DomElement.ATTRIBUTE_NOT_DEFINED && !codebase.endsWith("/")) {
				codebase += "/";
			}

			// Resolve base path.
			String basePath = getPageLinksForCurrentContext().getBaseUrl();
			if (!(codebase == null || codebase.isEmpty())) {
				basePath = codebase;
			}

			// Save archives if present.
			if (archives != null) {
				for (String arch : archives) {
					String urlTemp = UrlUtils.resolveUrl(basePath, arch);
					processEmbeddedFile(urlTemp);
				}
			}

			// Save embedded file if provided attribute name exists.
			String urlTemp = UrlUtils.resolveUrl(basePath, attributeNameChecked);
			processEmbeddedFile(urlTemp);
		}
	}

	private void processEmbeddedMultimediaFile(HtmlElement element, String attributeName) throws IOException, ParameterException,
			ResourceException, StorageException {
		// Ignore future BASE tags.
		getPageLinksForCurrentContext().ignoreBaseTag();

		if (taskParams.isSaveMultimedia()) {
			// Save embedded file if provided attribute name exists.
			String attributeNameChecked = element.getAttribute(attributeName);
			if (attributeNameChecked != DomElement.ATTRIBUTE_NOT_DEFINED) {
				processEmbeddedFile(UrlUtils.resolveUrl(getPageLinksForCurrentContext().getBaseUrl(), attributeNameChecked));
			}
		}
	}

	private void processEmbeddedImageFile(HtmlElement element, String attributeName) throws IOException, ParameterException,
			ResourceException, StorageException {
		// Ignore future BASE tags.
		getPageLinksForCurrentContext().ignoreBaseTag();

		if (taskParams.isSaveImages()) {
			// Save embedded file if provided attribute name exists.
			String attributeNameChecked = element.getAttribute(attributeName);
			if (attributeNameChecked != DomElement.ATTRIBUTE_NOT_DEFINED) {
				processEmbeddedFile(UrlUtils.resolveUrl(getPageLinksForCurrentContext().getBaseUrl(), attributeNameChecked));
			}
		}
	}

	private void processEmbeddedOtherFile(HtmlElement element, String attributeName) throws IOException, ParameterException,
			ResourceException, StorageException {
		// Ignore future BASE tags, for all tags but HTML.
		if (!element.getTagName().equalsIgnoreCase(HTML_STRING)) {
			// Basically, this is happening only when tag is HTML and attribute
			// is MANIFEST.
			getPageLinksForCurrentContext().ignoreBaseTag();
		}

		if (taskParams.isSaveOthers()) {
			// Save embedded file if provided attribute name exists.
			String attributeNameChecked = element.getAttribute(attributeName);
			if (attributeNameChecked != DomElement.ATTRIBUTE_NOT_DEFINED) {
				processEmbeddedFile(UrlUtils.resolveUrl(getPageLinksForCurrentContext().getBaseUrl(), attributeNameChecked));
			}
		}
	}

	private void processEmbeddedFile(String urlOfFileToSave) throws IOException, ParameterException, ResourceException, StorageException {
		boolean isSubContextOpened = true;
		try {
			wc.setJavaScriptEnabled(false);
			prepareSubPage(urlOfFileToSave, WebClientOrigin.EMBEDDED);
			
			// Checks for illegal characters in URI.
			new Link(urlOfFileToSave, "");
			
			ProcessedPage processedPage = new ProcessedPage(wc.getPage(urlOfFileToSave));
			processPage(processedPage);
		} catch (java.net.URISyntaxException e) {
			addNoHostFailedInfoToEmbeddedUrlObject(e.getMessage());
			LOGGER.warn(e.getMessage());
			LOGGER.debug(e.getMessage(), e);
		} catch (ContextSizeLimitExceeded e) {
			isSubContextOpened = false;
			LOGGER.warn(e.getMessage());
			LOGGER.debug(e.getMessage(), e);
		} catch (java.net.UnknownHostException e) {
			addNoHostFailedInfoToEmbeddedUrlObject("Host not found: " + e.getMessage());
			LOGGER.warn(e.getMessage());
			LOGGER.debug(e.getMessage(), e);
		} catch (org.apache.commons.httpclient.URIException e) {
			addNoHostFailedInfoToEmbeddedUrlObject(e.getMessage());
			LOGGER.warn(e.getMessage());
			LOGGER.debug(e.getMessage(), e);
		} catch(org.apache.http.conn.HttpHostConnectException e) {
			addNoHostFailedInfoToEmbeddedUrlObject("Connection error: " + e.getMessage());
			LOGGER.warn(e.getMessage());
			LOGGER.debug(e.getMessage(), e);
		} catch (Exception e) {
			addNoHostFailedInfoToEmbeddedUrlObject("Unknown error while processing embedded resource: " + urlOfFileToSave + "; " + e.getMessage());
			LOGGER.warn("Exception while saving embedded file: " + urlOfFileToSave);
			LOGGER.debug(e.getMessage(), e);
		} finally {
			if (isSubContextOpened) {
				ctx.closeSubContext();
			}
			wc.setJavaScriptEnabled(true);
		}
	}

	/**
	 * Adds additional attributes to new object when embedded resource host does
	 * not exists.
	 */
	private void addNoHostFailedInfoToEmbeddedUrlObject(String message) {
		ctx.addAttribute("active", false);
		ctx.addAttribute("reason_failed", message);
		if (taskParams.isAddReferrer()) {
			String referrer = ctx.getCurrentContextServiceData().getInputReferrer();
			if (referrer != null) {
				ctx.addAttribute("referrer", referrer);
			} else {
				ctx.addAttribute("referrer", "");
			}
		}
		if (taskParams.isAddReferrerCookie()) {
			Long referrerCookieId = ctx.getCurrentContextServiceData().getInputReferrerCookieId();
			if (referrerCookieId != null) {
				ctx.addReference("referrer_cookie", referrerCookieId);
			} else {
				ctx.addReference("referrer_cookie", -1L);
			}
		}
		try {
			RequestWrapper rw = new RequestWrapper(ctx.getServiceData().getInputUrlOriginal(), ctx.getServiceData().getUrlForProcessing(), null);
			long referenceId = ctx.saveInDataStore(rw);
			ctx.addReference("http_request", referenceId);
		} catch (RequiredParameterMissingException e) {
			LOGGER.warn("Couldn't create HTTP request wrapper, parameter missing.", e);
		} catch (StorageException e) {
			LOGGER.warn("Couldn't write HTTP request object to Data Store.", e);
		} catch (ParameterException e) {
			LOGGER.warn("Invalid parameter while writting HTTP request object to Data Store.", e);
		}
	}

	private void validateSupportedProtocols(String absoluteUri) throws URIException {
		Pattern pattern = Pattern.compile("^([a-zA-Z][a-zA-Z\\-_].+?):");
		Matcher matcher = pattern.matcher(absoluteUri);
		String scheme = "";
		if (matcher.find()) {
			scheme = matcher.group(1);
		}
		if (!(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
			throw new URIException(absoluteUri);
		} 
	}

	public PageLinks getPageLinksForCurrentContext() {
		return ctx.getPageLinks();
	}

	public Long getCookiesReferenceIdForCurrentContext() {
		return ctx.getCookiesReferenceId();
	}

	/**
	 * Check if provided URL is valid.
	 * 
	 * @param url URL to check.
	 * @return True if URL is valid, false otherwise.
	 */
	private boolean properUrl(String url) {
		try {
			String encoded = URIUtil.encode(url, Link.PROPER_URL_BITSET); 			
			new URL(encoded);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	// FIXME:zmienic na getInsecurePagesChain i zwracac ProcessedPage
	public Page getInsecurePage(String url) throws IOException, ExecutionException, TimeoutException {
		final WebRequest req = insecurePageInitialization(url);
		long processingTime = System.currentTimeMillis();
		ExecutorService ex = Executors.newSingleThreadExecutor();
		Future<Page> f = ex.submit(new Callable<Page>() {
			@Override
			public Page call() throws IOException {
				return wc.getPage(req);
			}
		});
		Page page = null;
		try {
			if (!interruptProcessing){
				if (taskParams.getPageTimeoutMillis() <= 0) {
					page = f.get();
				}
				else {
					page = f.get(taskParams.getPageTimeoutMillis(), TimeUnit.MILLISECONDS);
				}
			}
		} catch (InterruptedException e) {
			LOGGER.warn("Gathering {} interrupted", url);
			Thread.currentThread().interrupt();
		} catch (java.util.concurrent.TimeoutException e) {
			throw new TimeoutException("Timeout when gathering (" + taskParams.getPageTimeoutMillis() + " ms):" + url, e);
		} finally {
			if (f != null) {
				f.cancel(true);
			}
			closeExecutorWithJSDisabled(ex);
		}
		processingTime = System.currentTimeMillis() - processingTime;
		insecurePagePostprocessing(url, processingTime, page);
		return page;
	}

	private WebRequest insecurePageInitialization(String url) throws TimeoutException, MalformedURLException {
		if (interruptProcessing) {
			throw new TimeoutException("Overall time limit exceeded url:" + url);
		}
		try {
			wc.setUseInsecureSSL(true);
		} catch (GeneralSecurityException e) {
			LOGGER.error(e.getMessage(), e);
		}
		final WebRequest req = new WebRequest(UrlUtils.toUrlUnsafe(url));

		// work-around for bug with deflated content.
		req.setAdditionalHeader("Accept-Encoding", "");
		return req;
	}

	private void insecurePagePostprocessing(String url, long sTime, Page p) throws TimeoutException, IOException {
		if (interruptProcessing || Thread.currentThread().isInterrupted()) {
			if (wc.getWebConnection() instanceof HttpWebConnection) {
				((HttpWebConnection) wc.getWebConnection()).shutdown();
			}
			throw new TimeoutException("Overall processing time limit exceeded");
		}

		if (p == null) {
			LOGGER.warn("Retrieving [{}] failed, spent: {}.{} sec.", new Object[] { url, sTime / ONE_SECOND_IN_MILISECONDS,
					sTime % ONE_SECOND_IN_MILISECONDS });
			throw new IOException("Couldn't retrieve page " + url);
		}

		LOGGER.debug("Retrieved [{}] in: {}.{} sec.[interruptProcessing={}]", new Object[] { url, sTime / ONE_SECOND_IN_MILISECONDS,
				sTime % ONE_SECOND_IN_MILISECONDS, interruptProcessing });
	}

	void addCookie(Cookie cookie) {
		wc.getCookieManager().addCookie(cookie);
	}

	Set<Cookie> getCookies() {
		return wc.getCookieManager().getCookies();
	}

	ScriptInterceptor getInterceptor() {
		return scriptInterceptor;
	}

	public void closeAllWindows() {
		try {
			stopJavaScripts();
			wc.closeAllWindows();
		} catch (Exception e) {
			LOGGER.warn(e.getMessage(), e);
		}
	}

	public void setContextData(WebClientWorker webClientWorker, ServiceParameters params, String urlForProcessing) {
		ctx.setServiceParams(params);
		ctx.setWebClientWorker(webClientWorker);
	}

	private void addRequiredAttributesToCurrentContext(ProcessedPage processedPage) throws ParameterException, ResourceException,
			StorageException {
		try {
			RequestWrapper requestWrapper = composeRequest(processedPage);
			long referenceId = ctx.saveInDataStore(requestWrapper);
			ctx.addReference("http_request", referenceId);
			String referrer = ctx.getCurrentContextServiceData().getInputReferrer();
			if (referrer != null) {
				ctx.addAttribute("referrer", referrer);
			}
			boolean isSuccessfull = processedPage != null && processedPage.isComplete();
			ctx.addAttribute("active", isSuccessfull);
			if (isSuccessfull) {
				addAttrsForSuccessfulProcessing(processedPage);
			} else {
				addAttrsForFailedProcessing();
			}
		} catch (StackOverflowError e) {
			ctx.addWarning("Serious problem with JVM - cannot recover task");
		} catch (NullPointerException e) {
			LOGGER.debug("NPE while processing task", e);
			String msg = e.getMessage();
			if (msg == null) {
				String url = ctx.getCurrentContextServiceData() != null ? ctx.getCurrentContextServiceData().getUrlForProcessing() : null;
				msg = "NullPointerException while processing " + url;
			}
			ctx.addAttribute("reason_failed", msg);
		}
	}

	private void addAttrsForFailedProcessing() {
		ctx.addAttribute("reason_failed", "Unable to access page content");
		if (workerDispatcher.getWarning() != null) {
			ctx.addWarning(workerDispatcher.getWarning());
			LOGGER.warn("Adding warning to Task : {}", workerDispatcher.getWarning());
		}
	}

	private void addAttrsForSuccessfulProcessing(ProcessedPage processedPage) throws StorageException, ResourceException,
			ParameterException {
		ctx.addAttribute("http_code", processedPage.getResponseCode());
		ctx.addAttribute(HTML_STRING, processedPage.isHtml());
		if (processedPage.isHtml()) {
			if (taskParams.isSaveHtml()) {
				InputStream content = null;
				content = processedPage.getContentAsStream();
				long referenceId = ctx.saveInDataStore(content);
				ctx.addReference("html_source", referenceId);
			}
			handleCookies();
		} else {
			// It's not HTML so download as single file if possible.
			downloadAndStoreSingleFile(processedPage);
			handleCookies();
		}
	}

	private RequestWrapper composeRequest(ProcessedPage processedPage) throws RequiredParameterMissingException {
		if (processedPage == null) {
			return new RequestWrapper(ctx.getCurrentContextServiceData().getInputUrlOriginal(), ctx.getCurrentContextServiceData().getUrlForProcessing(), null);
		} else if (workerDispatcher.isSuccessfull()) {
			return new RequestWrapper(processedPage.getOriginalUrl(), processedPage.getRequestedUrl().toExternalForm(), processedPage.getRequestHeaders(),
					processedPage.getResponseCode(), processedPage.getResponseHeaders());
		} else {
			return new RequestWrapper(processedPage.getOriginalUrl(), processedPage.getRequestedUrl().toExternalForm(), processedPage.getRequestHeaders());
		}
	}

	private void handleCookies() throws StorageException {
		if (taskParams.isSaveCookies() && workerDispatcher.getCookies().size() != 0) {
			ctx.saveCookiesInDataStore(getComposedCookies());
			ctx.addReference("cookie_list", ctx.getCookiesReferenceId());
		}
	}

	private Set<CookieWrapper> getComposedCookies() throws StorageException {
		Set<CookieWrapper> cookieWrappers = workerDispatcher.getCookies();
		if (ctx.getInputDataInputReferrerCookieId() != null) {
			cookieWrappers.addAll(ctx.getCookiesFromDataStore(ctx.getInputDataInputReferrerCookieId()));
		}
		return cookieWrappers;
	}

	/**
	 * Used when reported URL is not HTML page but some other file.
	 * 
	 * @param processedPage
	 * @throws StorageException
	 * @throws ParameterException
	 * @throws ResourceException
	 */
	private void downloadAndStoreSingleFile(ProcessedPage processedPage) throws StorageException, ParameterException, ResourceException {
		String urlForProcessing = processedPage.getRequestedUrl().toExternalForm();
		InputStream contentStream = processedPage.getContentAsStream();
		String contentType = processedPage.getContentType();
		long savedContentId = ctx.saveInDataStore(contentStream);

		// Process PDF, SWF or other file.
		WebClientObjectType objectType = WebClientObjectType.forMimeType(contentType);
		if (objectType.isElliglibeForExtract(taskParams) && processedPage.getResponseCode() == HttpStatus.SC_OK) {
			try {
				NewWebClientUrlObject newWebClientUrlObject = new NewWebClientUrlObject(
						urlForProcessing,
						null,
						objectType.getName(),
						contentType,
						taskParams.isAddReferrer() ? ctx.getCurrentContextServiceData().getInputReferrer() : null,
								taskParams.isAddReferrerCookie() ? ctx.getInputDataInputReferrerCookieId() : null,
										savedContentId);
				ctx.newObject(newWebClientUrlObject);
			} catch (URIException e) {
				LOGGER.warn("Not an URL!: {}, msg={}", urlForProcessing, e.getMessage());
			}
		}
	}

	public Map<String, Map<String, ScriptElement>> getLaunchedScripts(){
		return scriptInterceptor.getSourcesByOrigin();
	}

	public void closeJsInterceptor() {
		LOGGER.debug("Closing javascript debugger/interceptor");
		scriptInterceptor.disableProcessing();
	}

	public WebClient getWc() {
		return wc;
	}
}
