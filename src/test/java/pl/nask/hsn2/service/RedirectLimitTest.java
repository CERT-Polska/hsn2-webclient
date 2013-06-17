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

import org.testng.annotations.Test;

import pl.nask.hsn2.ContextSizeLimitExceededException;
import pl.nask.hsn2.service.task.WebClientTaskContext;

public class RedirectLimitTest {
	
	@Test
	public void testTreeHeightLimitNotExceeded() throws Exception {
		WebClientTaskContext ctx = new WebClientTaskContext(1, 2, 3, null);
		
		int limit = 2;
				
		ServiceParameters params = new ServiceParameters();
		params.setRedirectDepthLimit(limit);
		ctx.setServiceParams(params);
		
		for (int i = 0; i < limit; i++)
			ctx.openSubContext();
	}
	
	@Test(expectedExceptions=ContextSizeLimitExceededException.class)
	public void testTreeHeightLimitExceeded() throws Exception {
		WebClientTaskContext ctx = new WebClientTaskContext(1, 2, 3, null);
		
		int limit = 1;
				
		ServiceParameters params = new ServiceParameters();
		params.setRedirectDepthLimit(limit);
		ctx.setServiceParams(params);
		
		for (int i = 0; i < limit; i++)
			ctx.openSubContext();
		ctx.openSubContext();
	}
	
	@Test
	public void testGlobalLimitNotExceeded() throws Exception {
		WebClientTaskContext ctx = new WebClientTaskContext(1, 2, 3, null);
		
		int limit = 3;
				
		ServiceParameters params = new ServiceParameters();
		params.setRedirectTotalLimit(limit);
		ctx.setServiceParams(params);
		
		
		for (int i = 0; i < limit; i++) {
			ctx.openSubContext();
			ctx.closeSubContext();
		}
	}
	
	@Test(expectedExceptions=ContextSizeLimitExceededException.class)
	public void testGlobalLimitExceeded() throws Exception {
		WebClientTaskContext ctx = new WebClientTaskContext(1, 2, 3, null);
		
		int limit = 3;
				
		ServiceParameters params = new ServiceParameters();
		params.setRedirectTotalLimit(limit);
		ctx.setServiceParams(params);
		
		// this will create a tree with full size
		for (int i = 0; i < limit; i++) {
			ctx.openSubContext();
			ctx.closeSubContext();
		}
		
		// this will exceed the limit
		ctx.openSubContext();
	}
	
}
