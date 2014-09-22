package com.ntechinternational.slap;

import java.util.Map;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

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
		
		//if valid visitor id has been provided
		//TODO: check if the test is valid, and meets the requirements
		if(visitorId > 0){
			
			//Step 1: CheckTokenId : gets the valid token id for the visitor id
			Token token = new TokenValidator().checkTokenId(visitorId);
			System.out.println(token);
			
			if(!questionResponse.equals("")){
				processedResponse = processQuestionRequest(visitorId, questionResponse);
			}
			else if(!itemIds.equals("")){
				processedResponse = processItemRequest(visitorId, itemIds);
			}
			else{
				processedResponse = processGeneralRequest(visitorId);
			}
		}
		
		return processedResponse;
		
	}

	
	/**
	 * This class processes query request where visitorId is provide
	 * @param visitorId
	 */
	private SlapResponse processGeneralRequest(long visitorId){
		
		return SlapResponse.createResponse();
	}
	
	
	private SlapResponse processQuestionRequest(long visitorId, String questionResponse){
		
		//split question response into name value pairs
		//example: questionResponse=sell:p101#client:c301
		Map<String, String> questionNVP = new Parser().getNameValuePairs(questionResponse);
		System.out.println(questionNVP);
		
		return SlapResponse.createResponse();
	}
	
	private SlapResponse processItemRequest(long visitorId, String itemIds){
		
		
		
		return SlapResponse.createResponse();
	}
}
