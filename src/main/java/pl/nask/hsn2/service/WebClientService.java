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
import pl.nask.hsn2.ServiceMain;
import pl.nask.hsn2.service.task.TaskContextFactoryImpl;
import pl.nask.hsn2.task.TaskFactory;

/**
 * Starter for the WebCrawler service.
 */
public final class WebClientService extends ServiceMain {
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
		wcs.serviceRunner.join();
		wcs.stop();
		wcs.destroy();
	}

	

		service = new GenericService(new WebClientTaskFactory(), new TaskContextFactoryImpl(), cmd.getMaxThreads(),
				cmd.getRbtCommonExchangeName(), cmd.getRbtNotifyExchangeName());
		

			
	}

	@Override
	protected CommandLineParams parseArguments(String[] args) {
		CommandLineParams cmd = new CommandLineParams();
		cmd.setDefaultServiceNameAndQueueName("webclient");
		cmd.parseParams(args);
		return cmd;
	}

	@Override
	protected void prepareService() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected TaskFactory createTaskFactory() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
