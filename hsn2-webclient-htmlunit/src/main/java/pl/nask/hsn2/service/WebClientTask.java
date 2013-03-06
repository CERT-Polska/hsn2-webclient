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

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.nask.hsn2.ParameterException;
import pl.nask.hsn2.RequiredParameterMissingException;
import pl.nask.hsn2.ResourceException;
import pl.nask.hsn2.StorageException;
import pl.nask.hsn2.service.task.WebClientTaskContext;
import pl.nask.hsn2.service.urlfollower.HtmlUnitFollower;
import pl.nask.hsn2.service.urlfollower.UrlFollower;
import pl.nask.hsn2.task.Task;
import pl.nask.hsn2.wrappers.CookieWrapper;
import pl.nask.hsn2.wrappers.RequestWrapper;

public class WebClientTask implements Task {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebClientTask.class);

    private WebClientTaskContext jobContext;
    private ServiceData inputData;
    private UrlFollower follower;

     public WebClientTask(WebClientTaskContext jobContext, ServiceParameters parameters, ServiceData inputData, UrlFollower follower) {
		this.jobContext = jobContext;
		this.jobContext.setServiceData(inputData);
		this.jobContext.setServiceParams(parameters);
		this.jobContext.webContextInit(inputData.getUrlForProcessing(), inputData.getInputReferrer(), inputData.getInputReferrerCookieId());
		this.inputData = inputData;
		this.follower = follower;
    }

	public WebClientTask(WebClientTaskContext jobContext, ServiceParameters params, ServiceData inputData, String urlForProcessing, String inputUrlOriginal) {
		this(jobContext, params, inputData, new HtmlUnitFollower(urlForProcessing, inputUrlOriginal, jobContext, params));
	}

    @Override
	public final boolean takesMuchTime() {
		return true;
	}

    private void addFailedInfoToJobContext() {
		jobContext.addAttribute("active", false);
		jobContext.addAttribute("reason_failed", follower.getFailureMessage());
		try {
			RequestWrapper rw = new RequestWrapper(inputData.getInputUrlOriginal(), inputData.getUrlForProcessing(), null);
			long referenceId = jobContext.saveInDataStore(rw);
			jobContext.addReference("http_request", referenceId);
		} catch (RequiredParameterMissingException e) {
			LOGGER.warn("Couldn't create HTTP request wrapper, parameter missing.", e);
		} catch (StorageException e) {
			LOGGER.warn("Couldn't write HTTP request object to Data Store.", e);
		} catch (ParameterException e) {
			LOGGER.warn("Invalid parameter while writting HTTP request object to Data Store.", e);
		}
    }
    
	@Override
	public final void process() throws ParameterException, ResourceException, StorageException {
		try {
			prepareCookies();
			follower.processUrl();
			if (!follower.isSuccessfull()) {
				addFailedInfoToJobContext();
			}
		} catch (StackOverflowError e) {
			jobContext.addWarning("Serious problem with JVM - cannot recover task");
		} catch (NullPointerException e) {
			String msg = e.getMessage();
			if (msg == null) {
				msg = "NullPointerException while processing " + inputData.getUrlForProcessing();
			}
			jobContext.addAttribute("reason_failed", msg);
			LOGGER.debug("NPE while processing task", e);
		} finally {
			follower = null;
		}
	}

    private void prepareCookies() throws StorageException {
        if (inputData.getInputReferrerCookieId() != null) {
            Set<CookieWrapper> cookieWrappers = jobContext.getCookiesFromDataStore(inputData.getInputReferrerCookieId());
            follower.setCookies(cookieWrappers);
        }
    }
}
