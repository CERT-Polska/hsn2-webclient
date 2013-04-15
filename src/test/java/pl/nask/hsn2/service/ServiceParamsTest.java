package pl.nask.hsn2.service;

import org.testng.Assert;
import org.testng.annotations.Test;

import pl.nask.hsn2.ParameterException;

public class ServiceParamsTest {
	@Test
	public void profileTest() throws ParameterException {
		ServiceParameters params = new ServiceParameters();
		Assert.assertEquals(params.getProfile(), "Firefox 3.6");
		
		params.setProfile("Internet Explorer 6");
		Assert.assertEquals(params.getProfile(), "Internet Explorer 6");
	}
}
