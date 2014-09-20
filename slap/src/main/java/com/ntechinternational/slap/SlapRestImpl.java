package com.ntechinternational.slap;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;

@Path("/processrequest")
public class SlapRestImpl {
	
	/**
	 * This is the main web service method that processes all the various request and provides a response
	 * @param visitorId the visitor id
	 * @return the string response
	 */
	@GET
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
	public SlapResponse processRequest(
			  @DefaultValue("-1") @QueryParam("visitorId") long visitorId
			, @DefaultValue("") @QueryParam("questionResponse") String questionResponse
			, @DefaultValue("") @QueryParam("itemIds") String itemIds){
		
		SlapResponse processedResponse = null;
		
		if(!questionResponse.equals("")){
			processedResponse = processQuestionRequest(visitorId, questionResponse);
		}
		else if(!itemIds.equals("")){
			processedResponse = processItemRequest(visitorId, itemIds);
		}
		else{
			processedResponse = processGeneralRequest(visitorId);
		}
		
		return processedResponse;
		
	}

	
	/**
	 * This class processes query request where visitorId is provide
	 * @param visitorId
	 */
	private SlapResponse processGeneralRequest(long visitorId){
		SlapResponse resp = new SlapResponse();
		
		return resp;
	}
	
	
	private SlapResponse processQuestionRequest(long visitorId, String questionResponse){
		SlapResponse resp = new SlapResponse();
		
		return resp;
	}
	
	private SlapResponse processItemRequest(long visitorId, String itemIds){
		
		SlapResponse resp = new SlapResponse();
		
		return resp;
	}
}
