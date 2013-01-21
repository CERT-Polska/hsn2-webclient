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

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mockit.Delegate;
import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import pl.nask.hsn2.ParameterException;
import pl.nask.hsn2.ResourceException;
import pl.nask.hsn2.ServiceConnector;
import pl.nask.hsn2.StorageException;
import pl.nask.hsn2.bus.operations.Attribute;
import pl.nask.hsn2.bus.operations.AttributeType;
import pl.nask.hsn2.bus.operations.ObjectData;
import pl.nask.hsn2.bus.operations.ObjectResponse;
import pl.nask.hsn2.bus.operations.builder.ObjectResponseBuilder;
import pl.nask.hsn2.server.TestHttpServer;
import pl.nask.hsn2.service.task.WebClientTaskContext;
import pl.nask.hsn2.service.urlfollower.HtmlUnitFollower;
import pl.nask.hsn2.service.urlfollower.ScriptInterceptor.ScriptElement;
import pl.nask.hsn2.utils.DataStoreHelper;
import pl.nask.hsn2.utils.WebClientDataStoreHelper;
import pl.nask.hsn2.wrappers.CookieWrapper;
import pl.nask.hsn2.wrappers.JSContextWrapper;

public class RedirectTest {
	private static final int MORE_THEN_ONE_JS_SAVED = 3;
	private static final String REFERRER = "http://referrer";
	private static final Set<CookieWrapper> COOKIE_WRAPPERS = new HashSet<CookieWrapper>();
	
	@Mocked
	ServiceConnector connector;
	
	WebClientTaskContext jobContext;
	private WebClientTask webClientTask;
	private ServiceParameters params;
	private HtmlUnitFollower follower;
	private ServiceData serviceData;

	@BeforeClass
	public void startServer() throws Exception {
		TestHttpServer.startServer("frames");
	}

	@AfterClass
	public void stopServer() throws Exception {
		TestHttpServer.stopServer();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@BeforeMethod
	public void beforeMethod() throws Exception {
		params = new ServiceParameters();
		params.setPageTimeoutMillis(200000);
		params.setSaveCookies(false);
		params.setSaveHtml(false);
		
		jobContext = new WebClientTaskContext(1, 1, 1, connector);
		new NonStrictExpectations() {
			@SuppressWarnings("unused")
			final WebClientDataStoreHelper wcHelper = null;
			{
				WebClientDataStoreHelper.getCookiesFromDataStore(connector, anyLong, anyLong);
				result = COOKIE_WRAPPERS;
				WebClientDataStoreHelper.saveInDataStore(connector, anyLong, withInstanceOf(Set.class));
				
				WebClientDataStoreHelper.saveJSContextsInDataStore(connector, anyLong, null);
				result = new Delegate() {
					@SuppressWarnings("unused")
					public long saveJSContextsInDataStore(ServiceConnector 
							 connector, long jobId, List<JSContextWrapper> jsContextWrappers){
						if(jsContextWrappers.size() > 1){
							return MORE_THEN_ONE_JS_SAVED;
						} else {
							String scriptCode = jsContextWrappers.get(0).getSource();
							 if(scriptCode.contains("foo()")){
								return 1;
							}else if(scriptCode.contains("alert('test')")){
								return 2;
							}
						}
						return -1;
					}
				};
			}
		};
		new NonStrictExpectations() {
			@SuppressWarnings("unused")
			final DataStoreHelper forTestOnlyDataStoreHelper = null;
			{
				pl.nask.hsn2.utils.DataStoreHelper.saveInDataStore(connector, anyLong, withInstanceOf(InputStream.class));
				result = 1l;
			}
		};
	}

	@Test
	public void testIframeFromJSRedirect() throws Exception {
		String testPageAbsoluteUrl = TestHttpServer.absoluteUrl("frameFromJSRedirect.html");
		serviceData = new ServiceData(1l, testPageAbsoluteUrl, testPageAbsoluteUrl, REFERRER, 1l, 5, 1l);
		follower = new HtmlUnitFollower(testPageAbsoluteUrl, jobContext, params);
		webClientTask = new WebClientTask(jobContext, params, serviceData, follower);
		webClientTask.process();
		
		Map<String, Map<String, ScriptElement>>  scriptsByOrigin = jobContext.getLaunchedScriptsByOrigin();
		
		Assert.assertEquals(scriptsByOrigin.size(), 2);
		Assert.assertEquals(scriptsByOrigin.get(testPageAbsoluteUrl).size(), 1);
    	Assert.assertEquals(scriptsByOrigin.get(TestHttpServer.absoluteUrl("frame.html")).size(), 1);
		Assert.assertEquals(follower.getPageLinks().getRedirects().size(), 0);
	}

	@Test
	public void testIframeRedirect() throws Exception {
		String testPageAbsoluteUrl = TestHttpServer.absoluteUrl("frameRedirect.html");
		serviceData = new ServiceData(1l, testPageAbsoluteUrl, testPageAbsoluteUrl, REFERRER, 1l, 5, 1l);
		follower = new HtmlUnitFollower(testPageAbsoluteUrl, jobContext, params);
		webClientTask = new WebClientTask(jobContext, params, serviceData, follower);
		webClientTask.process();
		
		Map<String, Map<String, ScriptElement>>  scriptsByOrigin = jobContext.getLaunchedScriptsByOrigin();
    	
		Assert.assertEquals(scriptsByOrigin.size(), 2, "should be 2 origin");
    	Assert.assertEquals(scriptsByOrigin.get(TestHttpServer.absoluteUrl("frame.html")).size(), 1);
		Assert.assertEquals(follower.getPageLinks().getRedirects().size(), 0);
	}

	@Test
	public void testServerRedirectToIframe() throws Exception {
		String testPageAbsoluteUrl = TestHttpServer.absoluteUrl("movedContext/frameRedirect.html");
		serviceData = new ServiceData(1l, testPageAbsoluteUrl, testPageAbsoluteUrl, REFERRER, 1l, 5, 1l);
		follower = new HtmlUnitFollower(testPageAbsoluteUrl, jobContext, params);
		webClientTask = new WebClientTask(jobContext, params, serviceData, follower);
		webClientTask.process();

		Map<String, Map<String, ScriptElement>>  scriptsByOrigin = jobContext.getLaunchedScriptsByOrigin();
    	
		Assert.assertEquals(scriptsByOrigin.size(), 2);
    	Assert.assertEquals(scriptsByOrigin.get(TestHttpServer.absoluteUrl("frame.html")).size(), 1);
		Assert.assertEquals(follower.getPageLinks().getRedirects().size(), 0);
	}

	@Test
	public void testIframeServerRedirect() throws Exception {
		String testPageAbsoluteUrl = TestHttpServer.absoluteUrl("frameWithServerRedirect.html");
		serviceData = new ServiceData(1l, testPageAbsoluteUrl, testPageAbsoluteUrl, REFERRER, 1l, 5, 1l);
		follower = new HtmlUnitFollower(testPageAbsoluteUrl, jobContext, params);
		webClientTask = new WebClientTask(jobContext, params, serviceData, follower);
		webClientTask.process();

		Map<String, Map<String, ScriptElement>>  scriptsByOrigin = jobContext.getLaunchedScriptsByOrigin();
    	
		Assert.assertEquals(scriptsByOrigin.size(), 1);
    	Assert.assertEquals(scriptsByOrigin.get(TestHttpServer.absoluteUrl("newLocation/frame.html")).size(), 1);
		Assert.assertEquals(follower.getPageLinks().getRedirects().size(), 0);
	}
	
	@Test
	public void testJsInWithRightContext() throws Exception {
		String testPageAbsoluteUrl = TestHttpServer.absoluteUrl("frameRedirect.html");
		String jsOnloadFullName = "onload event for HtmlBody[<body onload=\"alert('test');\">] in " + testPageAbsoluteUrl;
		
		serviceData = new ServiceData(1l, testPageAbsoluteUrl, testPageAbsoluteUrl, REFERRER, 1l, 5, 1l);
		follower = new HtmlUnitFollower(testPageAbsoluteUrl, jobContext, params);
		webClientTask = new WebClientTask(jobContext, params, serviceData, follower);
		webClientTask.process();
		
		Map<String, Map<String, ScriptElement>>  scriptsByOrigin = jobContext.getLaunchedScriptsByOrigin();
    	
		Assert.assertEquals(scriptsByOrigin.size(), 2,"invalid origin count");
    	Assert.assertEquals(scriptsByOrigin.get(TestHttpServer.absoluteUrl("frame.html")).size(), 1,"invalid js count in frame.html");
    	Assert.assertTrue(scriptsByOrigin.get(testPageAbsoluteUrl).containsKey(jsOnloadFullName), testPageAbsoluteUrl + " not contain onload action");
    	Assert.assertEquals(follower.getPageLinks().getRedirects().size(), 0);
	}
	
	@Test
	public void testJsRefInRightContext() throws Exception {
		
		new NonStrictExpectations() {    		    		
	        {	      
	        	connector.saveObjects(anyLong, withInstanceOf(List.class)); 
	        	result = new Delegate() {
	        		public ObjectResponse saveObjects(long jobId, List<ObjectData> objects) {
	        			ObjectResponseBuilder b = new ObjectResponseBuilder(pl.nask.hsn2.bus.operations.ObjectResponse.ResponseType.SUCCESS_PUT);
	        			for (int i=1; i <= objects.size(); i++) {
	        				b.addObject(i);
	        			}
	        			return b.build();
	        		}
				};
	        }
		};
		
		String testPageAbsoluteUrl = TestHttpServer.absoluteUrl("frameRedirect.html");
		
		serviceData = new ServiceData(1l, testPageAbsoluteUrl, testPageAbsoluteUrl, REFERRER, 1l, 5, 1l);
		follower = new HtmlUnitFollower(testPageAbsoluteUrl, jobContext, params);
		webClientTask = new WebClientTask(jobContext, params, serviceData, follower);
		webClientTask.process();
		jobContext.flush();
		Assert.assertTrue(follower.isSuccessfull());
		Assert.assertEquals(jobContext.getContextByUrl(testPageAbsoluteUrl).getObjectDataBuilder().build().findAttribute("js_context_list", AttributeType.BYTES).getBytes().getKey(), 2, "invalid script in " + testPageAbsoluteUrl + " object");
		Assert.assertEquals(jobContext.getContextByUrl(TestHttpServer.absoluteUrl("frame.html")).getObjectDataBuilder().build().findAttribute("js_context_list", AttributeType.BYTES).getBytes().getKey(), 1, "invalid script in frame.html object");
		
	}
	
	@Test(enabled=true)
    public void jsFromFileTest() throws ParameterException, ResourceException, StorageException {
    	String TEST_PAGE = "scriptFromFile.html";
    	String baseUrl = TestHttpServer.absoluteUrl(TEST_PAGE);


    	follower = new HtmlUnitFollower(baseUrl, jobContext, params);
    	webClientTask = new WebClientTask(jobContext, params, new ServiceData(baseUrl,""),follower);
    	webClientTask.process();
    	jobContext.flush();
    	
    	Assert.assertTrue(follower.isSuccessfull());
//    	Assert.assertEquals(jobContext.getLaunchedScriptsByOrigin().size(), 1);
    	Assert.assertEquals(jobContext.getContextByUrl(baseUrl).getObjectDataBuilder().build().findAttribute("js_context_list", AttributeType.BYTES).getBytes().getKey(), MORE_THEN_ONE_JS_SAVED, "invalid scripts in " + baseUrl + " object");
		
    }
	
	@Test(enabled=true)
    public void jsFromFileServerRedirectTest() throws ParameterException, ResourceException, StorageException {
    	String TEST_PAGE = "jsFileServerRedirect.html";
    	String baseUrl = TestHttpServer.absoluteUrl(TEST_PAGE);


    	follower = new HtmlUnitFollower(baseUrl, jobContext, params);
    	webClientTask = new WebClientTask(jobContext, params, new ServiceData(baseUrl,""),follower);
    	webClientTask.process();
    	jobContext.flush();
    	
    	Assert.assertTrue(follower.isSuccessfull());
//    	Assert.assertEquals(jobContext.getLaunchedScriptsByOrigin().size(), 1);
    	Assert.assertEquals(jobContext.getContextByUrl(baseUrl).getObjectDataBuilder().build().findAttribute("js_context_list", AttributeType.BYTES).getBytes().getKey(), MORE_THEN_ONE_JS_SAVED, "invalid scripts in " + baseUrl + " object");
		
    }
	
	@Test(enabled=true)
    public void jsFromFrameWithoutNodeTest() throws ParameterException, ResourceException, StorageException {
		
		new NonStrictExpectations() {    		    		
	        {	      
	        	connector.saveObjects(anyLong, withInstanceOf(List.class)); 
	        	result = new Delegate() {
	        		public ObjectResponse saveObjects(long jobId, List<ObjectData> objects) {
	        			ObjectResponseBuilder b = new ObjectResponseBuilder(pl.nask.hsn2.bus.operations.ObjectResponse.ResponseType.SUCCESS_PUT);
	        			for (int i=1; i <= objects.size(); i++) {
	        				b.addObject(i);
	        			}
	        			return b.build();
	        		}
				};
	        }
		};
		String testPageAbsoluteUrl = TestHttpServer.absoluteUrl("frameRedirect.html");
		params.setRedirectDepthLimit(0);
		
		serviceData = new ServiceData(1l, testPageAbsoluteUrl, testPageAbsoluteUrl, REFERRER, 1l, 5, 1l);
		follower = new HtmlUnitFollower(testPageAbsoluteUrl, jobContext, params);
		webClientTask = new WebClientTask(jobContext, params, serviceData, follower);
		webClientTask.process();
		Assert.assertEquals(jobContext.getLaunchedScriptsByOrigin().size(), 2);
		jobContext.flush();
		Assert.assertTrue(follower.isSuccessfull());
		Assert.assertEquals(jobContext.getContextByUrl(testPageAbsoluteUrl).getObjectDataBuilder().build().findAttribute("js_context_list", AttributeType.BYTES).getBytes().getKey(), MORE_THEN_ONE_JS_SAVED, "invalid scripts in " + testPageAbsoluteUrl + " object");
    }
	
	@Test(enabled=true)
    public void jsFromRedirectAndFrameWithoutNodeTest() throws ParameterException, ResourceException, StorageException {
    	
		new NonStrictExpectations() {    		    		
	        {	      
	        	connector.saveObjects(anyLong, withInstanceOf(List.class)); 
	        	result = new Delegate() {
	        		public ObjectResponse saveObjects(long jobId, List<ObjectData> objects) {
	        			ObjectResponseBuilder b = new ObjectResponseBuilder(pl.nask.hsn2.bus.operations.ObjectResponse.ResponseType.SUCCESS_PUT);
	        			for (int i=1; i <= objects.size(); i++) {
	        				b.addObject(i);
	        			}
	        			return b.build();
	        		}
				};
	        }
		};
		String testPageAbsoluteUrl = TestHttpServer.absoluteUrl("movedContext/frameRedirect.html");
		params.setRedirectDepthLimit(1);
		
		serviceData = new ServiceData(1l, testPageAbsoluteUrl, testPageAbsoluteUrl, REFERRER, 1l, 5, 1l);
		follower = new HtmlUnitFollower(testPageAbsoluteUrl, jobContext, params);
		webClientTask = new WebClientTask(jobContext, params, serviceData, follower);
		webClientTask.process();
		
		Assert.assertTrue(follower.isSuccessfull());
		Assert.assertEquals(jobContext.getLaunchedScriptsByOrigin().size(), 2);
		jobContext.flush();
//		Assert.assertEquals(jobContext.getLaunchedScriptsByOrigin().size(), 1);
		
		Attribute jsAttribute = jobContext.getContextByUrl(testPageAbsoluteUrl).getObjectDataBuilder().build().findAttribute("js_context_list", AttributeType.BYTES);
		Assert.assertNotNull(jsAttribute, "root does not have js ref");
		Assert.assertEquals(jsAttribute.getBytes().getKey(), 1, "invalid scripts in " + testPageAbsoluteUrl + " object.");
    }
	
	@Test
	public void testAttributeInRedirect() throws Exception {
		String testPageAbsoluteUrl = TestHttpServer.absoluteUrl("movedContext/frameRedirect.html");
		serviceData = new ServiceData(1l, testPageAbsoluteUrl, testPageAbsoluteUrl, REFERRER, 1l, 5, 1l);
		follower = new HtmlUnitFollower(testPageAbsoluteUrl, jobContext, params);
		webClientTask = new WebClientTask(jobContext, params, serviceData, follower);
		webClientTask.process();
		
		ObjectData rootObjectData = jobContext.getContextByUrl(testPageAbsoluteUrl).getObjectDataBuilder().build();
		Assert.assertEquals(rootObjectData.findAttribute("http_code", AttributeType.INT).getInteger(), 302, "invalid response code in root");
		Assert.assertEquals(rootObjectData.findAttribute("html", AttributeType.BOOL).getBool(), false, "invalid html attribute in root");
		
		String firstChildUrl = TestHttpServer.absoluteUrl("newLocation/frameRedirect.html");
		ObjectData firstChildObjectData = jobContext.getContextByUrl(firstChildUrl).getObjectDataBuilder().build();
		Assert.assertEquals(firstChildObjectData.findAttribute("http_code", AttributeType.INT).getInteger(), 200, "invalid response code in child");
		Assert.assertEquals(firstChildObjectData.findAttribute("url_original", AttributeType.STRING).getString(), firstChildUrl, "invalid original url in child");
		Assert.assertEquals(firstChildObjectData.findAttribute("origin", AttributeType.STRING).getString(), "redirect", "invalid origin in child");
		
		String secondChildUrl = TestHttpServer.absoluteUrl("frame.html");
		ObjectData secondChildObjectData = jobContext.getContextByUrl(secondChildUrl).getObjectDataBuilder().build();
		Assert.assertEquals(secondChildObjectData.findAttribute("http_code", AttributeType.INT).getInteger(), 200, "invalid response code in child");
		Assert.assertEquals(secondChildObjectData.findAttribute("url_original", AttributeType.STRING).getString(), secondChildUrl, "invalid original url in child");
		Assert.assertEquals(secondChildObjectData.findAttribute("origin", AttributeType.STRING).getString(), "iframe", "invalid origin in child");
		
	}
}
