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

import java.lang.Thread.UncaughtExceptionHandler;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonController;
import org.apache.commons.daemon.DaemonInitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.nask.hsn2.CommandLineParams;
import pl.nask.hsn2.GenericService;
import pl.nask.hsn2.service.task.TaskContextFactoryImpl;

/**
 * Starter for the WebCrawler service.
 */
public final class WebClientService implements Daemon {
	private static final int JOIN_WAIT_TIME = 10000;
	private static final int ERR_EXIT_CODE = 128;
	private static final Logger LOGGER = LoggerFactory.getLogger(WebClientService.class);
	private Thread serviceRunner = null;
	private CommandLineParams cmd = null;
	private GenericService service = null;

	public static void main(final String[] args) throws DaemonInitException, InterruptedException {
		WebClientService wcs = new WebClientService();

		wcs.init(new DaemonContext() {

			@Override
			public DaemonController getController() {
				return null;
			}

			@Override
			public String[] getArguments() {
				return args;
			}
		});
		wcs.start();
		while (!wcs.serviceRunner.isInterrupted()) {
			Thread.sleep(1000l);
		}
		wcs.stop();
		wcs.destroy();
	}

	@Override
	public void init(DaemonContext context) throws DaemonInitException {
		cmd = new CommandLineParams();
		cmd.setDefaultServiceNameAndQueueName("webclient");
		cmd.parseParams(context.getArguments());

		service = new GenericService(new WebClientTaskFactory(), new TaskContextFactoryImpl(), cmd.getMaxThreads(),
				cmd.getRbtCommonExchangeName(), cmd.getRbtNotifyExchangeName());
		cmd.applyArguments(service);
		serviceRunner = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
						@Override
						public void uncaughtException(Thread t, Throwable e) {
							LOGGER.error(e.getMessage(), e);
							System.exit(ERR_EXIT_CODE);
						}
					});
					service.run();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}, "webclient-service");
		LOGGER.info("WebClient service initialized");
	}

	@Override
	public void start() {
		serviceRunner.start();
		LOGGER.info("Service started");
	}

	@Override
	public void stop() throws InterruptedException {
		serviceRunner.interrupt();
		serviceRunner.join(JOIN_WAIT_TIME);
		LOGGER.info("WebClient stopped.");
	}

	@Override
	public void destroy() {
		LOGGER.info("WebClient destroyed.");
	}
}
