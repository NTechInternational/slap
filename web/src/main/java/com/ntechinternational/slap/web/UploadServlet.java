package com.ntechinternational.slap.web;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

public class UploadServlet extends HttpServlet {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
	
		if(ServletFileUpload.isMultipartContent(request)){
			
			//ensure file is csv
			try {
                List<FileItem> multiparts = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(request);
              
                
                FileItem csvFile = getCSVFile(multiparts, "questionCSV", request);
            	if(csvFile != null){
            		SolrManager mgr = new SolrManager();
            		request.setAttribute("message", mgr.uploadQuestionToSolr(csvFile.getInputStream()));
            	}
            	else{
            		request.setAttribute("message", "Invalid file provided");
            	}
            } catch (Exception ex) {
               request.setAttribute("message", "File Upload Failed due to " + ex);
            }
		}
		else{
            request.setAttribute("message",
                                 "Sorry this Servlet only handles file upload request");
        }
    
		request.getRequestDispatcher("/result.jsp").forward(request, response);
	}

	private FileItem getCSVFile(List<FileItem> multiparts, String paramName, HttpServletRequest request) {
		FileItem retItem = null;
		
		for(FileItem item : multiparts){
            if(!item.isFormField() && item.getFieldName().equals(paramName)){
            	if(item.getSize() != 0 && item.getContentType().equals("text/csv")){
        			retItem = item;
            	}
            	
            	break;
	        }
	    }
		
		return retItem;

	}
}
