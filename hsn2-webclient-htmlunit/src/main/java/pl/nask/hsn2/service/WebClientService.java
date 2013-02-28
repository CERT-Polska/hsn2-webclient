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

import pl.nask.hsn2.CommandLineParams;
import pl.nask.hsn2.GenericService;
import pl.nask.hsn2.service.task.TaskContextFactoryImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starter for the WebCrawler service.
 */
public final class WebClientService implements Daemon{
	private static final Logger LOGGER = LoggerFactory.getLogger(WebClientService.class);
    Thread serviceRunner = null;
    CommandLineParams cmd = null;
    GenericService service = null;

    public static void main(final String[] args) throws DaemonInitException, Exception {
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

	@Override
	public void init(DaemonContext context) throws DaemonInitException, Exception { 
		cmd = new CommandLineParams();
		cmd.setDefaultServiceNameAndQueueName("webclient");
		cmd.parseParams(context.getArguments());
		
		service = new GenericService(new WebClientTaskFactory(),  new TaskContextFactoryImpl(), cmd.getMaxThreads(), cmd.getRbtCommonExchangeName(), cmd.getRbtNotifyExchangeName());
		cmd.applyArguments(service);
		serviceRunner = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
						
						@Override
						public void uncaughtException(Thread t, Throwable e) {
							LOGGER.error(e.getMessage(),e);
							System.exit(128);
							
						}
					});
					service.run();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				
			}
		},"webclient-service");
		
		
	}

	@Override
	public void start() throws Exception {
		serviceRunner.start();
		
	}

	@Override
	public void stop() throws Exception {
		serviceRunner.interrupt();
		serviceRunner.join(10000);
		
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}
}