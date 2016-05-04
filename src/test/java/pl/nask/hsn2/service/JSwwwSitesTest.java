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

package pl.nask.hsn2.service;


import java.io.InputStream;
import java.util.BitSet;
import java.util.Map;

import mockit.NonStrictExpectations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import pl.nask.hsn2.ParameterException;
import pl.nask.hsn2.ResourceException;
import pl.nask.hsn2.ServiceConnector;
import pl.nask.hsn2.StorageException;
import pl.nask.hsn2.server.TestHttpServer;
import pl.nask.hsn2.service.task.WebClientTaskContext;
import pl.nask.hsn2.service.urlfollower.HtmlUnitFollower;
import pl.nask.hsn2.service.urlfollower.ScriptInterceptor.ScriptElement;
import pl.nask.hsn2.utils.DataStoreHelper;

@Test(groups="JSscripts")
public class JSwwwSitesTest {
    private static final String REL_URL = "jsTests";
	private HtmlUnitFollower follower;
	private WebClientTask webClientTask;
	private final int JS_TIMEOUT = 15000;
	private ServiceParameters params;
	private WebClientTaskContext wcTaskContext;

    @BeforeClass    
    public void initServer() throws Exception {
        TestHttpServer.startServer(REL_URL);
    }

    @AfterClass
    public void stopServer() throws Exception {
        TestHttpServer.stopServer();
    }

    @Test//(enabled=true, timeOut=JS_TIMEOUT + 1000)
    public void testRecursiveDownloadJS() throws ParameterException, ResourceException, StorageException {
    	String TEST_PAGE="index.html";

    	String baseUrl = TestHttpServer.absoluteUrl(TEST_PAGE);

    	follower = new HtmlUnitFollower(baseUrl, wcTaskContext, params);
    	webClientTask = new WebClientTask(wcTaskContext, params, new ServiceData(TestHttpServer.absoluteUrl("index.html"),""),follower);
    	webClientTask.process();
    	
    	Assert.assertEquals(follower.getPageLinks().getOutgoingLinks().size(),1);
    }
    
    @Test(enabled=true, timeOut=JS_TIMEOUT + 1000)
    public void js1Test() throws StorageException, ParameterException, ResourceException {
    	String TEST_PAGE="js1.html";
    	
    	String baseUrl = TestHttpServer.absoluteUrl(TEST_PAGE);

    	follower = new HtmlUnitFollower(baseUrl, wcTaskContext, params);
    	webClientTask = new WebClientTask(wcTaskContext, params, new ServiceData(baseUrl,""),follower);
    	webClientTask.process();
    	
    	Assert.assertTrue(follower.isSuccessfull(), "successful");
    	Map<String, Map<String, ScriptElement>>  scriptsByOrigin = wcTaskContext.getLaunchedScriptsByOrigin();
    	
    	Assert.assertEquals(scriptsByOrigin.size(), 1);
    	Assert.assertEquals(scriptsByOrigin.get(baseUrl).size(), 2);
    	Assert.assertEquals(follower.getPageLinks().getOutgoingLinks().size(),1);
		
    }
    
    @Test(enabled=true, timeOut=JS_TIMEOUT + 1000)
    public void js2Test() throws ParameterException, ResourceException, StorageException {
    	String TEST_PAGE="js2.html";
    	String baseUrl = TestHttpServer.absoluteUrl(TEST_PAGE);


    	follower = new HtmlUnitFollower(baseUrl, wcTaskContext, params);
    	webClientTask = new WebClientTask(wcTaskContext, params, new ServiceData(baseUrl,""),follower);
    	webClientTask.process();
    	
    	Assert.assertTrue(follower.isSuccessfull());
    	Map<String, Map<String, ScriptElement>>  scriptsByOrigin = wcTaskContext.getLaunchedScriptsByOrigin();
    	
    	Assert.assertEquals(scriptsByOrigin.size(), 1);
    	Assert.assertEquals(scriptsByOrigin.get(baseUrl).size(), 2);
    	Assert.assertEquals(follower.getPageLinks().getOutgoingLinks().size(),1);

    }

    @Test(enabled=true, timeOut=JS_TIMEOUT * 2)
    public void js3Test() throws ParameterException, ResourceException, StorageException {
    	String TEST_PAGE="js3.html";
    	String baseUrl = TestHttpServer.absoluteUrl(TEST_PAGE);

    	follower = new HtmlUnitFollower(baseUrl, wcTaskContext, params);
    	webClientTask = new WebClientTask(wcTaskContext, params, new ServiceData(baseUrl,""),follower);
    	webClientTask.process();
    	
    	
    	Assert.assertTrue(follower.isSuccessfull(),"should be successful");
    	Map<String, Map<String, ScriptElement>>  scriptsByOrigin = wcTaskContext.getLaunchedScriptsByOrigin();
    	
    	Assert.assertEquals(scriptsByOrigin.size(), 1);
    	Assert.assertEquals(scriptsByOrigin.get(baseUrl).size(), 3,"javascripts incorrectly reported");
    	Assert.assertEquals(follower.getPageLinks().getOutgoingLinks().size(),1);
    	

    }
    
    
    @Test(enabled=true, timeOut=JS_TIMEOUT + 1000)
    public void js4Test() throws StorageException, ParameterException, ResourceException {
    	String TEST_PAGE="js4.html";
    	String baseUrl = TestHttpServer.absoluteUrl(TEST_PAGE);
    	
    	// js4.html contains a simple loop so 2 seconds should be enough for this test
    	params.setSingleJsTimeoutMillis(2000);
    	follower = new HtmlUnitFollower(baseUrl, wcTaskContext, params);
    	webClientTask = new WebClientTask(wcTaskContext, params, new ServiceData(baseUrl,""),follower);
    	webClientTask.process();
    	
    	Map<String, Map<String, ScriptElement>>  scriptsByOrigin = wcTaskContext.getLaunchedScriptsByOrigin();
    	
    	Assert.assertEquals(scriptsByOrigin.size(), 1);
    	Assert.assertTrue(scriptsByOrigin.get(baseUrl).size() >= 2);
    	Assert.assertEquals(follower.getPageLinks().getOutgoingLinks().size(),1);
    	

    }

	@Test(enabled = true, timeOut = JS_TIMEOUT + 1000)
	public void js5Test() throws ParameterException, ResourceException, StorageException {
		String TEST_PAGE = "js5.html";
		String baseUrl = TestHttpServer.absoluteUrl(TEST_PAGE);

		follower = new HtmlUnitFollower(baseUrl, wcTaskContext, params);
		webClientTask = new WebClientTask(wcTaskContext, params, new ServiceData(baseUrl, ""), follower);
		webClientTask.process();

		Map<String, Map<String, ScriptElement>> scriptsByOrigin = wcTaskContext.getLaunchedScriptsByOrigin();

		Assert.assertEquals(scriptsByOrigin.size(), 1);
		Assert.assertEquals(scriptsByOrigin.get(baseUrl).size(), 4);

		// Below assertion is not required (according to specification and data
		// contract)
		Assert.assertEquals(follower.getPageLinks().getOutgoingLinks().size(), 1);
	}

    @Test(enabled=true, timeOut=JS_TIMEOUT + 1000)
    public void js6Test() throws ParameterException, ResourceException, StorageException {
    	String TEST_PAGE="js6.html";
    	String baseUrl = TestHttpServer.absoluteUrl(TEST_PAGE);
    	
    	params.setSingleJsTimeoutMillis(2000);
    	follower = new HtmlUnitFollower(baseUrl, wcTaskContext, params);
    	webClientTask = new WebClientTask(wcTaskContext, params, new ServiceData(baseUrl,""),follower);
    	webClientTask.process();
    	
    	Map<String, Map<String, ScriptElement>>  scriptsByOrigin = wcTaskContext.getLaunchedScriptsByOrigin();
    	
    	Assert.assertEquals(scriptsByOrigin.size(), 1);
    	Assert.assertEquals(scriptsByOrigin.get(baseUrl).size(), 3);
    	Assert.assertEquals(follower.getPageLinks().getOutgoingLinks().size(),1);
    }
    
    @Test(enabled=true, timeOut=JS_TIMEOUT + 1000)
    public void iisEncodedOutgoingLinkTest() throws ParameterException, ResourceException, StorageException {
    	String TEST_PAGE="IISencoded.html";
    	String baseUrl = TestHttpServer.absoluteUrl(TEST_PAGE);


    	follower = new HtmlUnitFollower(baseUrl, wcTaskContext, params);
    	webClientTask = new WebClientTask(wcTaskContext, params, new ServiceData(baseUrl,""),follower);
    	webClientTask.process();
    	
    	Assert.assertTrue(follower.isSuccessfull());
    	Assert.assertEquals(wcTaskContext.getLaunchedScriptsByOrigin().size(), 0);
    	Assert.assertEquals(follower.getPageLinks().getOutgoingLinks().size(),6);

    }
    @Test(enabled=true, timeOut=JS_TIMEOUT + 1000)
    public void js7Test() throws StorageException, ParameterException, ResourceException {
    	String TEST_PAGE="js7.html";
    	
    	String baseUrl = TestHttpServer.absoluteUrl(TEST_PAGE);

    	follower = new HtmlUnitFollower(baseUrl, wcTaskContext, params);
    	webClientTask = new WebClientTask(wcTaskContext, params, new ServiceData(baseUrl,""),follower);
    	webClientTask.process();
    	
    	Assert.assertTrue(follower.isSuccessfull(), "successful");
    	Map<String, Map<String, ScriptElement>>  scriptsByOrigin = wcTaskContext.getLaunchedScriptsByOrigin();
    	
    	Assert.assertEquals(scriptsByOrigin.size(), 1);
    	Assert.assertEquals(scriptsByOrigin.get(baseUrl).size(), 5);
    	Assert.assertEquals(follower.getPageLinks().getOutgoingLinks().size(),2);
		
    }
    @Test(enabled=true)//, timeOut=JS_TIMEOUT + 1000)
    public void js8Test() throws StorageException, ParameterException, ResourceException {
    	String TEST_PAGE="js8.html";
    	
    	String baseUrl = TestHttpServer.absoluteUrl(TEST_PAGE);

    	follower = new HtmlUnitFollower(baseUrl, wcTaskContext, params);
    	webClientTask = new WebClientTask(wcTaskContext, params, new ServiceData(baseUrl,""),follower);
    	webClientTask.process();
    	
    	Assert.assertTrue(follower.isSuccessfull(), "successful");
    	Map<String, Map<String, ScriptElement>>  scriptsByOrigin = wcTaskContext.getLaunchedScriptsByOrigin();
    	
    	Assert.assertEquals(scriptsByOrigin.size(), 1);
    	Assert.assertEquals(scriptsByOrigin.get(baseUrl).size(), 3);
    	Assert.assertTrue(follower.getPageLinks().getOutgoingLinks().size() > 0);
		
    }
    
    @Test(enabled=true)//, timeOut=JS_TIMEOUT + 100000)
    public void jsIdsTest() throws StorageException, ParameterException, ResourceException {
    	Logger log = LoggerFactory.getLogger("jsIdsTest");
    	log.debug("start");
    	
    	String TEST_PAGE="jsIdsTest.html";
    	
    	String baseUrl = TestHttpServer.absoluteUrl(TEST_PAGE);

    	follower = new HtmlUnitFollower(baseUrl, wcTaskContext, params);
    	webClientTask = new WebClientTask(wcTaskContext, params, new ServiceData(baseUrl,""),follower);
    	webClientTask.process();
    	
    	Assert.assertTrue(follower.isSuccessfull(), "successful");
    	Map<String, Map<String, ScriptElement>>  scriptsByOrigin = wcTaskContext.getLaunchedScriptsByOrigin();
    	
    	Assert.assertEquals(scriptsByOrigin.size(), 1, "scriptsByOrigin.size");
    	Assert.assertEquals(scriptsByOrigin.get(baseUrl).size(), 5, "number of scripts for the baseUrl");    	
    	
    	//see TASK#8322
	    	BitSet bs = new BitSet(11);
	    	bs.set(0);
	    	bs.set(3);
	    	bs.set(7);
	    	bs.set(9);
	    	bs.set(10);

    	for(ScriptElement se: scriptsByOrigin.get(baseUrl).values() ) {
    		Assert.assertTrue(bs.get(se.getId()), "BitSet[" + se.getId() +"]");
    	}
    	log.debug("end");
		
    }
    
    @BeforeMethod
	public void prepareTests() throws ParameterException, StorageException, ResourceException {
		params = new ServiceParameters();
    	params.setSaveObjects(true);
    	params.setSaveOthers(true);
    	params.setSaveJsContext(true);
    	params.setSingleJsTimeoutMillis(JS_TIMEOUT);
    	params.setPageTimeoutMillis(99999999);
    	params.setProcessingTimeout(999999999);
    	
    	wcTaskContext = new WebClientTaskContext(0, 0, 0, null);
    	new NonStrictExpectations() {
    		DataStoreHelper dh;
			{
				dh.saveInDataStore((ServiceConnector)any, anyLong,(byte[])any );result = 1l;
				dh.saveInDataStore((ServiceConnector)any, anyLong, (InputStream) any); result = 2l;
    		}
    	};
	}
}