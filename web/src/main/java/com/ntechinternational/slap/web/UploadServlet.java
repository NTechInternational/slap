package com.ntechinternational.slap.web;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang.StringEscapeUtils;

public class UploadServlet extends HttpServlet {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
		if(request.getParameter("clearAll") != null){
			try{
				new SolrManager().clearSolr();
				request.setAttribute("message", "All documents have been cleared");
			}
			catch(Exception ex){
				request.setAttribute("message", "Clear failed due to " + ex);
			}
			
		}
		else if(ServletFileUpload.isMultipartContent(request)){
			
    		
			//ensure file is csv
			try {
				SolrManager mgr = new SolrManager();
                List<FileItem> multiparts = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(request);
              
                
                FileItem csvFile = getCSVFile(multiparts, "questionCSV", request);
            	if(csvFile != null){
            		if(mgr.uploadQuestionToSolr(csvFile.getInputStream()))
        				request.setAttribute("message", "Successfully, uploaded the questions");
            		else
            			request.setAttribute("message", "Failed to upload the challenge");
            	}
            	else{
            		csvFile = getCSVFile(multiparts, "challengeCSV", request);
            		if(csvFile != null){
            			if(mgr.uploadChallengeToSolr(csvFile.getInputStream()))
        					request.setAttribute("message", "Successfully, uploaded the challenges");
            			else
            				request.setAttribute("message", "Failed to upload the challenge");
            		}
            		else{
            			request.setAttribute("message", "Invalid file provided");
            		}
            	}
            } catch (Exception ex) {
               request.setAttribute("message", "File Upload Failed due to " + ex);
            }
		}
		else{
            request.setAttribute("message",
                                 "Sorry this Servlet only handles file upload request");
        }
    
		request.getRequestDispatcher("/index.jsp").forward(request, response);
	}

	private FileItem getCSVFile(List<FileItem> multiparts, String paramName, HttpServletRequest request) {
		FileItem retItem = null;
		
		for(FileItem item : multiparts){
            if(!item.isFormField() && item.getFieldName().equals(paramName)){
            	if(item.getSize() != 0 ){ //&& item.getContentType().equals("text/csv") : chrome sets content type to application/octet-stream
        			retItem = item;
            	}
            	
            	break;
	        }
	    }
		
		return retItem;

	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
		
		try{
			if(request.getParameter("exportQuestions") != null){
				response.addHeader("Content-Disposition", "attachment; filename=\"questions.csv\"");
				response.addHeader("Content-Type", "text/csv");
				new SolrManager().exportQuestion(response.getOutputStream());
				response.getOutputStream().flush();
			}
			else if(request.getParameter("exportChallenges") != null){
				response.addHeader("Content-Disposition", "attachment; filename=\"challenges.csv\"");
				response.addHeader("Content-Type", "text/csv; charset=utf-8");
				new SolrManager().exportChallenge(response.getOutputStream());
				response.getOutputStream().flush();
			}
			else{
				request.getRequestDispatcher("/result.jsp").forward(request, response);
			}
		}
		catch(Exception ex){
			request.setAttribute("message",
                    ex.getMessage());
			
			request.getRequestDispatcher("/index.jsp").forward(request, response);
		}
		
		
	}
}
