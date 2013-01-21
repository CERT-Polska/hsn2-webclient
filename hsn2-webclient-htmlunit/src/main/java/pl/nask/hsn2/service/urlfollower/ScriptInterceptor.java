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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sourceforge.htmlunit.corejs.javascript.Context;
import net.sourceforge.htmlunit.corejs.javascript.EvaluatorException;
import net.sourceforge.htmlunit.corejs.javascript.debug.DebugFrame;
import net.sourceforge.htmlunit.corejs.javascript.debug.DebuggableScript;
import net.sourceforge.htmlunit.corejs.javascript.debug.Debugger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.nask.hsn2.service.ServiceParameters;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class ScriptInterceptor implements Debugger {

    private final static Logger LOGGER = LoggerFactory.getLogger(ScriptInterceptor.class);
    private final int jsRecursionLimit;
    private final Map<String, Map<String, ScriptElement>> scriptsByOrigin = new ConcurrentHashMap<String, Map<String, ScriptElement>>();
    private int scriptId = 0;
    private final boolean throwJsException;
    private Map<Integer, Integer> recursionCounter = null;
	private volatile boolean process = true;

    public ScriptInterceptor(ServiceParameters taskParams) {
        this.jsRecursionLimit = taskParams.getJsRecursionLimit();
        if (this.jsRecursionLimit >= 0) {
            this.throwJsException = true;
        } else {
            this.throwJsException = false;
        }
        if (this.throwJsException) {
            recursionCounter = new HashMap<Integer, Integer>();
        }
    }

    public ScriptInterceptor() {
        this.throwJsException = false;
        this.jsRecursionLimit = ServiceParameters.JS_RECURSION_LIMIT;
    }

    public static class ScriptElement {

        private final int id;
        private final String source;
        private final boolean isEval;

        public ScriptElement(int id, String source, boolean eval) {
            this.id = id;
            this.source = source;
            isEval = eval;
        }

        public int getId() {
            return id;
        }

        public String getSource() {
            return source;
        }

        public boolean isEval() {
            return isEval;
        }

        @Override
        public String toString() {
            return "ScriptElement[id=" + id + ", isEval=" + isEval + ", source=" + source + "]";
        }
    }

    @Override
    public void handleCompilationDone(Context context, DebuggableScript script, String source) {
    	if ( !process) {
    		LOGGER.debug("Processing disabled, no more scripts will collected");
    		EvaluatorException er = new EvaluatorException("JavaScript processing is stopped.source won't be processed:"+script.getSourceName());
    		Context.throwAsScriptRuntimeEx(er);
    		return;
    	}
        HtmlPage page = (HtmlPage) context.getThreadLocal("startingPage");
        String origin = page.getUrl().toString();
        String srcName = script.getSourceName();
        ScriptElement scriptElement = new ScriptElement(scriptId++, source, script.isGeneratedScript());
        Map<String, ScriptElement> scriptsFromOrgin = null;

        Integer hash = null;
        if (throwJsException) {
            StringBuilder sb = new StringBuilder()
                    .append(origin)
                    .append(source.hashCode())
                    .append(script.getParamCount())
                    .append(script.getFunctionName());
            hash = sb.toString().hashCode();
        }

        if (scriptsByOrigin.containsKey(origin)) {
            scriptsFromOrgin = scriptsByOrigin.get(origin);

        } else {
            scriptsFromOrgin = new ConcurrentHashMap<String, ScriptElement>();
            scriptsByOrigin.put(origin, scriptsFromOrgin);

        }
        if (!scriptsFromOrgin.containsKey(srcName)) {
            scriptsFromOrgin.put(srcName, scriptElement);
            LOGGER.debug("Adding new script: {}", srcName);
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Sources from {} contain {} already!,", new Object[]{origin, srcName});
            }
        }

        if (throwJsException) {
            Integer i = recursionCounter.get(hash);
            if (i == null) {
                i = Integer.valueOf(0);
            }
            if (i == jsRecursionLimit) {
                EvaluatorException er = new EvaluatorException("Recursive JavaScript call attempt(" + i + ").", srcName, -1);
                LOGGER.warn("Interrupting JavaScript execution:{},[hash:{}]called {} times, {}", new Object[]{srcName, hash, i, origin});
                Context.throwAsScriptRuntimeEx(er);
            } else {
                i++;
                recursionCounter.put(hash, i);
                LOGGER.debug("Added javascript to recursive call counter:[hash:{}],par:{},topLevel:{},funcName:{},called {} times.", new Object[]{hash, script.getParamCount(), script.isTopLevel(), script.getFunctionName(), i});
            }
        }
    }

    @Override
    public DebugFrame getFrame(Context cx, DebuggableScript fnOrScript) {
        return new JsScriptDebugFrame(cx, fnOrScript);
    }

    public Map<String, Map<String, ScriptElement>> getSourcesByOrigin() {
        return scriptsByOrigin;
    }

	public void disableProcessing() {
		process  = false;
		if(recursionCounter != null) {
			recursionCounter.clear();
		}
		recursionCounter = null;
		
		
	}
}
