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

import java.lang.Thread.UncaughtExceptionHandler;

public class WorkerThreadExceptionHandler implements UncaughtExceptionHandler {
	private HtmlUnitFollower owner;

	public WorkerThreadExceptionHandler(HtmlUnitFollower htmlUnitFollower) {
		owner = htmlUnitFollower;
	}

	@Override
	public final void uncaughtException(Thread t, Throwable e) {
		if (e instanceof StackOverflowError) {
			String msg = e.getMessage();
			if (msg == null) {
				msg = "[" + t.getName() + "] WebClient parser has crashed: " + e.getClass();
			}
			owner.handleJvmError(msg);
		} else {
			owner.handleJvmError(e.getMessage());
		}
	}

}
