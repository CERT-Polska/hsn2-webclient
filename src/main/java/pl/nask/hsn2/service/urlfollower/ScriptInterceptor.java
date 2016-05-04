/*
 * Copyright (c) NASK, NCSC
 *
 * This file is part of HoneySpider Network 2.1.
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sourceforge.htmlunit.corejs.javascript.Context;
import net.sourceforge.htmlunit.corejs.javascript.ContextInspector;
import net.sourceforge.htmlunit.corejs.javascript.EvaluatorException;
import net.sourceforge.htmlunit.corejs.javascript.debug.DebugFrame;
import net.sourceforge.htmlunit.corejs.javascript.debug.DebuggableScript;
import net.sourceforge.htmlunit.corejs.javascript.debug.Debugger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.nask.hsn2.service.ServiceParameters;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class ScriptInterceptor implements Debugger {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptInterceptor.class);
    private final int jsRecursionLimit;
    private final Map<String, Map<String, ScriptElement>> scriptsByOrigin = new ConcurrentHashMap<String, Map<String, ScriptElement>>();
    private int scriptId = 0;
	private volatile boolean process = true;

    public ScriptInterceptor(ServiceParameters taskParams) {
        jsRecursionLimit = taskParams.getJsRecursionLimit();
    }

    public ScriptInterceptor() {
    	jsRecursionLimit = ServiceParameters.JS_RECURSION_LIMIT;
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

        public final int getId() {
            return id;
        }

        public final String getSource() {
            return source;
        }

        public final boolean isEval() {
            return isEval;
        }

        @Override
        public final String toString() {
            return "ScriptElement[id=" + id + ", isEval=" + isEval + ", source=" + source + "]";
        }
    }

    @Override
    public final void handleCompilationDone(Context context, DebuggableScript script, String source) {
    	if ( !process) {
    		LOGGER.debug("Processing disabled, no more scripts will collected");
    		EvaluatorException er = new EvaluatorException("JavaScript processing is stopped.source won't be processed:"+script.getSourceName());
    		Context.throwAsScriptRuntimeEx(er);
    		return;
    	}

        String origin = getOriginForScript(context);

        Map<String, ScriptElement> scriptsFromOrgin = null;
        if (scriptsByOrigin.containsKey(origin)) {
            scriptsFromOrgin = scriptsByOrigin.get(origin);
        } else {
            scriptsFromOrgin = new ConcurrentHashMap<String, ScriptElement>();
            scriptsByOrigin.put(origin, scriptsFromOrgin);
        }

        String srcName = script.getSourceName();
        ScriptElement scriptElement = new ScriptElement(scriptId++, source, script.isGeneratedScript());
        if (!scriptsFromOrgin.containsKey(srcName)) {
            scriptsFromOrgin.put(srcName, scriptElement);
            LOGGER.debug("Adding new script: {}", srcName);
        } else {
        	LOGGER.debug("Sources from {} contain {} already!,", new Object[]{origin, srcName});
        }

        if (jsRecursionLimit >= 0) {
            int depth = ContextInspector.getDepth(context);
            if (depth >= jsRecursionLimit || checkScriptDepth(script)) {
                EvaluatorException er = new EvaluatorException("Recursive JavaScript call attempt(" + depth + ").", srcName, -1);
                LOGGER.warn("Interrupting JavaScript execution:{}, stack depth: {}, {}", new Object[]{srcName, depth, origin});
                Context.throwAsScriptRuntimeEx(er);
            }
        }
    }

    private String getOriginForScript(Context context){
    	HtmlPage page = (HtmlPage) context.getThreadLocal("startingPage");
        return page.getUrl().toString();
    }

	@Override
    public final DebugFrame getFrame(Context cx, DebuggableScript fnOrScript) {
		return new JsScriptDebugFrame();
    }

    public final Map<String, Map<String, ScriptElement>> getSourcesByOrigin() {
        return scriptsByOrigin;
    }

	public final void disableProcessing() {
		process  = false;
	}

	private boolean checkScriptDepth(DebuggableScript script){
		DebuggableScript parent = script.getParent();
		int i = 0;
		while(parent != null){
			i++;
			if(i < jsRecursionLimit){
				parent = parent.getParent();
			}
			else{
				return true;
			}
		}
		//LOGGER.info(""+i);
		return false;
	}
}
