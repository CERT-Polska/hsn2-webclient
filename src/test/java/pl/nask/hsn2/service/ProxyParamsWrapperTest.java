package pl.nask.hsn2.service;

import org.testng.Assert;
import org.testng.annotations.Test;

import pl.nask.hsn2.service.urlfollower.ProxyParamsWrapper;

public class ProxyParamsWrapperTest {

	@Test
	public void socksWithUserTest() {
		ProxyParamsWrapper pp = new ProxyParamsWrapper("socks://user:pass@łóbódóbó.pl:8080");
		Assert.assertTrue(pp.isSocksProxy());
		Assert.assertEquals(pp.getPort(), 8080);
		Assert.assertEquals(pp.getHost(), "xn--bdb-fnabbb93d.pl");
		Assert.assertTrue(pp.hasUserCredentials());
		Assert.assertEquals(pp.getUserName(), "user");
		Assert.assertEquals(pp.getUserPswd(), "pass");
		
	}
	
	@Test
	public void socksNoUserTest() {
		ProxyParamsWrapper pp = new ProxyParamsWrapper("socks://localhost:8080");
		Assert.assertTrue(pp.isSocksProxy());
		Assert.assertEquals(pp.getPort(), 8080);
		Assert.assertEquals(pp.getHost(), "localhost");
		Assert.assertFalse(pp.hasUserCredentials());
	}
	@Test
	public void httpDefaultPortTest() {
		ProxyParamsWrapper pp = new ProxyParamsWrapper("http://localhost");
		Assert.assertTrue(pp.isProxy() && pp.isHttpProxy());
		Assert.assertFalse(pp.hasUserCredentials());
		Assert.assertEquals(pp.getHost(),"localhost");
		Assert.assertEquals(pp.getPort(), 80);
	}
}
