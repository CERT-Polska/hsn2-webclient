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

import org.apache.commons.daemon.DaemonInitException;

import pl.nask.hsn2.CommandLineParams;
import pl.nask.hsn2.ServiceMain;
import pl.nask.hsn2.service.task.TaskContextFactoryImpl;
import pl.nask.hsn2.task.TaskContextFactory;
import pl.nask.hsn2.task.TaskFactory;

/**
 * Starter for the WebCrawler service.
 */
public final class WebClientService extends ServiceMain {
	public static void main(final String[] args) throws DaemonInitException, InterruptedException {
		WebClientService wcs = new WebClientService();
		wcs.init(new DefaultDaemonContext(args));
		wcs.start();
	}

	protected TaskContextFactory createTaskContextFactory() {
		return new TaskContextFactoryImpl();
	}
	
	@Override
	protected void prepareService() {
	}

	@Override
	protected TaskFactory createTaskFactory() {
		return new WebClientTaskFactory();
	}
	
	@Override
	protected CommandLineParams newCommandLineParams() {
		CommandLineParams cmd = new CommandLineParams();
		cmd.setDefaultServiceNameAndQueueName("webclient");
		return cmd;
	}
}
