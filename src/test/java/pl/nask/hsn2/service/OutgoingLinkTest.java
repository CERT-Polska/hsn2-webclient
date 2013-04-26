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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

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
import pl.nask.hsn2.bus.operations.ObjectResponse;
import pl.nask.hsn2.connector.REST.DataResponse;
import pl.nask.hsn2.server.TestHttpServer;
import pl.nask.hsn2.service.task.WebClientTaskContext;
import pl.nask.hsn2.service.urlfollower.HtmlUnitFollower;

public class OutgoingLinkTest {
	WebClientTaskContext jobContext;

	private ServiceParameters params;
	private WebClientTask webClientTask;
    private HtmlUnitFollower follower;

    @Mocked
	private ServiceConnector connector;

    @BeforeClass
    public void initServer() throws Exception{
    	TestHttpServer.startServer("outgoingLinks");
	}

    @AfterClass
    public void stopServer() throws Exception {
        TestHttpServer.stopServer();
    }

    @BeforeMethod
    public void beforeMethod() throws ParameterException {
    	params = new ServiceParameters();
    	jobContext = new WebClientTaskContext(0,0,0,connector);
    	follower = new HtmlUnitFollower(TestHttpServer.getWebserverRoot(), jobContext, params);
    	webClientTask = new WebClientTask(jobContext, params, new ServiceData("",""), follower);
    }

    @Test
	public void testProcessExternalLinks() throws Exception{
    	params.setProcessExternalLinks(0);
    	params.setLinkLimit(100);
    	linkOFFExpectations();
    	webClientTask.process();
    	jobContext.flush();
    }

    @Test
	public void testLinkLimit() throws Exception{
    	params.setProcessExternalLinks(1);
    	params.setLinkLimit(0);
    	linkOFFExpectations();
    	webClientTask.process();
    	jobContext.flush();
    	Assert.assertTrue(follower.isSuccessfull());
    }

    @Test
	public void testAddLinks() throws Exception{
    	params.setProcessExternalLinks(1);
    	params.setLinkLimit(10);
    	linkONExpectations();
    	webClientTask.process();
    	jobContext.flush();
    	Assert.assertTrue(follower.isSuccessfull());
    }

    private void linkOFFExpectations() throws StorageException, ParameterException, ResourceException, IOException {
		final DataResponse dataResponse = new DataResponse(1L);
		final ObjectResponse saveObjectsResponse = new ObjectResponse(pl.nask.hsn2.bus.operations.ObjectResponse.ResponseType.SUCCESS_PUT);
		saveObjectsResponse.setObjects(Collections.singleton(1L));
    	new NonStrictExpectations() {
	        {
	        	connector.sendDataStoreData(anyLong, withInstanceOf(byte[].class));result=dataResponse;
	        	connector.sendDataStoreData(anyLong, withInstanceOf(InputStream.class));result=dataResponse;
	        	connector.saveObjects(anyLong, null);times=0;
	        }
		};
	}

    private void linkONExpectations() throws StorageException, ParameterException, ResourceException, IOException {
		final DataResponse dataResponse = new DataResponse(1L);
		final ObjectResponse saveObjectsResponse = new ObjectResponse(pl.nask.hsn2.bus.operations.ObjectResponse.ResponseType.SUCCESS_PUT);
		saveObjectsResponse.setObjects(Collections.singleton(1L));
    	new NonStrictExpectations() {
	        {
	        	connector.sendDataStoreData(anyLong, withInstanceOf(byte[].class));result=dataResponse;
	        	connector.sendDataStoreData(anyLong, withInstanceOf(InputStream.class));result=dataResponse;
	        	connector.saveObjects(anyLong, null);times=1;result=saveObjectsResponse;
	        	forEachInvocation = new Object() {
	        		@SuppressWarnings("unused")
					public void verify(long jobId, List<?> list) {
	        			Assert.assertEquals(list.size(), 10, "number of saved objects");
	        		}
	        	};
	        }
		};
	}
}
