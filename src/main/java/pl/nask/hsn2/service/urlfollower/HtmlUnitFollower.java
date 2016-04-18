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

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.nask.hsn2.service.ServiceParameters;
import pl.nask.hsn2.service.task.WebClientTaskContext;
import pl.nask.hsn2.wrappers.CookieWrapper;

/**
 * Hits the URL and fetches it's content. Provides basic info about content's mime-type, outgoing links (if it's a html
 * document) and so on.
 */
public class HtmlUnitFollower implements UrlFollower {
	private static AtomicLong procCounter = new AtomicLong();
    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlUnitFollower.class);
    private final Object lock = new Object();
    private final CountDownLatch latch;
    private WebClientWorker webClientWorker = null;
    private ServiceParameters params;
    private String urlForProcessing;
    private String originalUrl;
    private StringBuilder failureMessage = new StringBuilder();
    private String warningMessage;
    private Boolean failed = false;
    private ProcessedPage processedPage;

	public HtmlUnitFollower(String urlForProcessing, String originalUrl, WebClientTaskContext ctx, ServiceParameters params) {
    	this.urlForProcessing = urlForProcessing;
    	this.originalUrl = originalUrl;
    	this.params = params;
    	latch = new CountDownLatch(1);
    	webClientWorker = new WebClientWorker(this, latch, ctx, params);
    	ctx.setWebClientWorker(webClientWorker);
    }

    public HtmlUnitFollower(String urlForProcessing, WebClientTaskContext ctx,ServiceParameters params) {
    	this(urlForProcessing, urlForProcessing, ctx, params);
	}

	@Override
	public final void processUrl() {
		webClientWorker.setContextData(webClientWorker, params, urlForProcessing);
		LOGGER.debug("Starting processing: {}", urlForProcessing);
		try {
			Thread worker = new Thread(webClientWorker, "WebClientWorker-" + procCounter.incrementAndGet());
			worker.setUncaughtExceptionHandler(new WorkerThreadExceptionHandler(this));
			worker.start();

			boolean latchCounterReachedZero;
			if (params.getProcessingTimeout() > 0) {
				latchCounterReachedZero = latch.await(params.getProcessingTimeout(), TimeUnit.MILLISECONDS);
			} else {
				latch.await();
				latchCounterReachedZero = latch.getCount() == 0;
			}
			if (!latchCounterReachedZero) {
				requestFailed("Task interrupted because time limit exceeded: " + params.getProcessingTimeout());
				webClientWorker.stopProcessing();
				webClientWorker.closeAllWindows();

				// Wait for clean WC exit.
				worker.join();
			}
		} catch (InterruptedException e) {
			LOGGER.error("Task has been interrupted", e);
		} finally {
			webClientWorker.stopJavaScripts();
			webClientWorker.closeJsInterceptor();
			LOGGER.info("Finished processing: {}", urlForProcessing);
		}
	}

	public final void requestFailed(Throwable e) {
		String msg;
		if (e instanceof ExecutionException) {
			//get real cause of error
			Throwable t = e;
			while (t.getCause() != null) {
				t = t.getCause();
			}
			msg = t.toString();
		} else {
			msg = e.getMessage();
			if (msg == null || msg.isEmpty()) {
				if (e.getCause() != null){
					msg = e.getCause().getMessage();
					if (msg == null || msg.isEmpty()) {
						msg = "Unknown error";
					}
				}
				else {
					msg = "Unknown error";
				}
			}
		}
		requestFailed(msg);
	}

	public final void requestFailed(String msg) {
		synchronized (lock) {
			LOGGER.debug("Request failed: {}", msg);
			if (failureMessage.length() > 0) {
				failureMessage.append("\n");
			}
			failureMessage.append(msg);
			failed = true;
		}
	}

	@Override
	public final boolean isSuccessfull() {
		synchronized (lock) {
			return !failed;
		}
	}

    @Override
    public final String getFailureMessage() {
    	synchronized (lock) {
    		if ( failureMessage.length() > 0) {
    			return failureMessage.toString();
    		}
    		if (!failed) {
    			return null;
    		}
    		return "Couldn't determine failure reason or internal HtmlUnitFollower error";
    	}
    }

    @Override
    public final String getContentType() {
        return processedPage.getContentType();
    }

    public final String getOriginalUrl() {
        return originalUrl;
    }

    @Override
    public final String getUrlForProcessing() {
        return urlForProcessing;
    }

    @Override
    public final Set<CookieWrapper> getCookies() {
        return webClientWorker.getCookies();
    }

    @Override
    public final void setCookies(Set<CookieWrapper> cookies) {
    	webClientWorker.setCookiesForInitialization(cookies);
    }

	final void handleJvmError(String msg) {
		requestFailed(msg);
		warningMessage = "JVM has crashed due to dynamic content processing at URL: "+originalUrl;
	}

	@Override
	public final String getWarning() {
		return warningMessage;
	}

	/**
	 * for tests only
	 *
	 * @return
	 */
	public final PageLinks getPageLinks() {
		return webClientWorker.getPageLinksForCurrentContext();
	}

	public final void setPage(ProcessedPage processedPage) {
		this.processedPage = processedPage;

	}

	public final ProcessedPage getProcessedPage() {
		return processedPage;
	}


}
