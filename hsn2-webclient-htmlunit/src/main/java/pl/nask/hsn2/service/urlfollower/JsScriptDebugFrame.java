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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import net.sourceforge.htmlunit.corejs.javascript.Context;
import net.sourceforge.htmlunit.corejs.javascript.Scriptable;
import net.sourceforge.htmlunit.corejs.javascript.debug.DebugFrame;
import net.sourceforge.htmlunit.corejs.javascript.debug.DebuggableScript;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsScriptDebugFrame implements DebugFrame {
	private static final Logger LOGGER = LoggerFactory.getLogger(JsScriptDebugFrame.class);
	private final static boolean IS_DEBUG_ENABLED = LOGGER.isDebugEnabled();
	private static AtomicLong l = new AtomicLong(0);
	private static List<Throwable> errors = Collections.synchronizedList(new ArrayList<Throwable>());

	public JsScriptDebugFrame(Context cx, DebuggableScript fnOrScript) {
		// does nothing
	}

	@Override
	public void onEnter(Context cx, Scriptable activation, Scriptable thisObj, Object[] args) {
		if (IS_DEBUG_ENABLED) {
			LOGGER.debug("Executing:[{}] {}", l.incrementAndGet(), activation);
		}
	}

	@Override
	public void onLineChange(Context cx, int lineNumber) {
		if (IS_DEBUG_ENABLED) {
			LOGGER.debug("lineChange {}", lineNumber);
		}
	}

	@Override
	public void onExceptionThrown(Context cx, Throwable ex) {
		if (IS_DEBUG_ENABLED) {
			LOGGER.debug("Got script exception: {}", ex.getMessage());
			errors.add(ex);
		}
	}

	@Override
	public void onExit(Context cx, boolean byThrow, Object resultOrException) {
		if (IS_DEBUG_ENABLED && resultOrException != null)
			LOGGER.debug("FINISHED function:[{}] {}", l.decrementAndGet(), resultOrException.toString());
	}

	@Override
	public void onDebuggerStatement(Context cx) {
	}

	public static void resetCounter() {
		if (IS_DEBUG_ENABLED)
			l.set(0);
	}
}
