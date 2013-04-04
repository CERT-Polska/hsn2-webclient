package pl.nask.hsn2.service;

import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.Assert;

import mockit.Mocked;

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.HttpStatus;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.HandlerWrapper;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import pl.nask.hsn2.ParameterException;
import pl.nask.hsn2.ResourceException;
import pl.nask.hsn2.ServiceConnector;
import pl.nask.hsn2.StorageException;
import pl.nask.hsn2.server.TestHttpServer;
import pl.nask.hsn2.service.task.WebClientTaskContext;
import pl.nask.hsn2.service.urlfollower.HtmlUnitFollower;


public class ProxyRequestTest {
	private static final String	REQ_PAGE = "http://google.pl/nonexistent.swf";
	private static String PROXY_URI = null;
	private static String lastProxyAuth = null;
	private WebClientTaskContext	jobContext;
	
	@Mocked
	private ServiceConnector serviceConnector;
	private ServiceParameters	params;
	private HtmlUnitFollower	urlFollower;
	
	static class ReqHandler extends HandlerWrapper implements Handler {
		
		@Override
		public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException,
				ServletException {
			response.setContentType("text/html");
			response.setStatus(HttpServletResponse.SC_OK);
			Request req = (Request)request;
			lastProxyAuth = (String)req.getAttribute("Proxy-Authorization");
			response.getWriter().println("<head><title>"+lastProxyAuth+"</title></head>");
			response.getWriter().println("<body>"+new Date()+"</body>");
			req.setHandled(true);
			super.handle(target, request, response, dispatch);
		}

	}
	
	@BeforeClass
	void setServer() throws Exception {
		TestHttpServer.startServerWithHandlers(new Handler[]{new ReqHandler()});
		PROXY_URI = "http://user:pass@localhost:"+TestHttpServer.getWebserverPort();
		
	}
	@AfterClass
	void stopServer() throws Exception {	
		TestHttpServer.stopServer();
	}
	
	@Test
	public void httpProxyTest() throws ParameterException, ResourceException, StorageException {
		initTask();
		ServiceData inputData = new ServiceData(1l, REQ_PAGE, REQ_PAGE,"", null, 10,null, PROXY_URI);
		WebClientTask task = new WebClientTask(jobContext, params, inputData, urlFollower) ;
		task.process();
		Assert.assertTrue(urlFollower.isSuccessfull());
		Assert.assertEquals(urlFollower.getContentType(), "text/html");
		Assert.assertTrue(urlFollower.getProcessedPage().getPage().getWebResponse().getStatusCode() == HttpStatus.ORDINAL_200_OK);

	}
	
	
	private void initTask() throws ParameterException {
		jobContext = new WebClientTaskContext(1l, 1, 1l, serviceConnector);
		params = new ServiceParameters();
		urlFollower = new HtmlUnitFollower(REQ_PAGE, jobContext, params);
	}

}
