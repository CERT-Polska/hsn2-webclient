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

package pl.nask.hsn2.wrappers;

import java.io.InputStream;
import java.util.List;

import pl.nask.hsn2.RequiredParameterMissingException;

public class FileWrapper {
    private List<RequestWrapper> requestWrappers;
    private String contentType;
    private Long savedContentId;
    private InputStream inputStream;

    public FileWrapper(List<RequestWrapper> requestWrappers, String contentType, InputStream input) throws RequiredParameterMissingException {
        this.requestWrappers = requestWrappers;
        this.contentType = contentType;
        this.inputStream = input;
        validate();
    }

    public FileWrapper(List<RequestWrapper> requestWrappers, String contentType, long savedContentId) throws RequiredParameterMissingException {
        this.requestWrappers = requestWrappers;
        this.contentType = contentType;
        this.savedContentId = savedContentId;
        validate();
    }

    private void validate() throws RequiredParameterMissingException {
    	if (contentType == null)
            throw new RequiredParameterMissingException("type parameter is required");
	}

	public List<RequestWrapper> getRequestWrappers() {
        return requestWrappers;
    }

    public String getContentType() {        
        return contentType;
    }

    public InputStream getContentStream() {
        return inputStream;
    }

    public Long getSavedContentId() {
        return savedContentId;
    }

	@Override
	public String toString() {
		return "FileWrapper [requestWrappers=" + requestWrappers
				+ ", contentType=" + contentType + ", savedContentId="
				+ savedContentId + "]";
	}
    
    
}
