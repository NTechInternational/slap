package com.ntechinternational.slap.web;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.FileItem;

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
            		request.setAttribute("message", "Valid file provided");
            	}
            	else{
            		request.setAttribute("message", "Invalid file provided");
            	}
            
                //if( && multiparts.size() > 0){
                //	request.setAttribute("message", "Importing Questions....");
                //}
           
               //File uploaded successfully
               //request.setAttribute("message", "File Uploaded Successfully");
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
            		try{
            			Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(new InputStreamReader(item.getInputStream()));
            			request.setAttribute("message", "Records found");
            			retItem = item;
            		}
            		catch(Exception ex){
            			//do nothing
            		}
            	}
            	
            	break;
	        }
	    }
		
		return retItem;

	}
}
