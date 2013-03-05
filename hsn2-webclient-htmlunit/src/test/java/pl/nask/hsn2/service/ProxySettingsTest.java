package pl.nask.hsn2.service;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.testng.annotations.Test;

import pl.nask.hsn2.server.TestHttpServer;

public class ProxySettingsTest {
  @Test
  public void httpProxyTest() throws Exception {
	  TestHttpServer.startServerWithHandlers(new Handler[]{new ReqHandler()});
	  Thread.currentThread().sleep(10000);
	  TestHttpServer.stopServer();
  }
  
  public static class ReqHandler implements Handler {

	private boolean	running;

	@Override
	public void start() throws Exception {
		running = true;
		
	}

	@Override
	public void stop() throws Exception {
		running = false;
		
	}

	@Override
	public boolean isRunning() {
		return running ;
	}

	@Override
	public boolean isStarted() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isStarting() {
		return running;
	}

	@Override
	public boolean isStopping() {
		return false;
	}

	@Override
	public boolean isStopped() {
		return running;
	}

	@Override
	public boolean isFailed() {
		return false;
	}

	@Override
	public void addLifeCycleListener(Listener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeLifeCycleListener(Listener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException,
			ServletException {
		System.out.println(request.getParameterMap().size());
		
	}

	@Override
	public void setServer(Server server) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Server getServer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}
	  
  }
}
