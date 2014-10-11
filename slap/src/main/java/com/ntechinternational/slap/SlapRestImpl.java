package com.ntechinternational.slap;

import java.net.UnknownHostException;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

@Path("/rest")
public class SlapRestImpl {
	
	private static final String TYPE_SUBMIT = "submit";
	private static final String TYPE_PARAM = "type";
	private static final String API_VERSION_PARAM = "apiversion";
	private static final String SOURCE_PARAM = "sourcetype";
	private static final String INVALID_VISITOR_ID_PROVIDED = "Invalid visitor ID provided";
	private static final String VISITOR_ID = "visitorId";
	private static final String MAP_FILENAME = "Map.xml";
	private static final String UNSUPPORTED_API_VERSION = "API Version unsupported, please provide correct apiversion parameter";
	private static final Object TYPE_SELECT = "select";
	
	/**
	 * This is the main web service method that processes all the various request and provides a response
	 * @param visitorId the visitor id
	 * @return the string response
	 */
	@GET
	@Produces({MediaType.APPLICATION_JSON})
	@Path("processrequest")
	public SlapResponse processRequest(@Context UriInfo uriInfo){
		
		SlapResponse processedResponse = new SlapResponse();;
		
		//if valid visitor id has been provided
		//TODO: check if the test is valid, and meets the requirements
		MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters(); 
		long visitorId = -1; //default null value
		int apiVersion = 0;
		try{
			visitorId = Long.parseLong(queryParams.getFirst(VISITOR_ID));
			processedResponse.visitorId = visitorId;
			
			//check the api version number is supported
			try{
				apiVersion = Integer.parseInt(queryParams.getFirst(API_VERSION_PARAM));
			}
			catch(NumberFormatException invalidVersion){
				apiVersion = 1; //set to current version level
			}
			
			if(apiVersion == 1){
				if(visitorId > 0){
					processedResponse = processRequest(visitorId, queryParams);
				}
				else{
					processedResponse.errorDescription = INVALID_VISITOR_ID_PROVIDED;
				}
			}
			else{
				processedResponse.errorDescription = UNSUPPORTED_API_VERSION; 
			}
		}
		catch(NumberFormatException ex){
			processedResponse.visitorId = 0;
			processedResponse.errorDescription = INVALID_VISITOR_ID_PROVIDED;
		}
		catch(Exception ex){
			System.err.println("An exception was occurred " + ex + " on " + ex.getStackTrace());
			processedResponse.errorDescription = ex.getMessage();
		}
		
		return processedResponse;
		
		
	}

	private SlapResponse processRequest(long visitorId,
		MultivaluedMap<String, String> queryParams) throws Exception {
		SlapResponse response = new SlapResponse();
		response.visitorId = visitorId;
		
		//Step 1: Validate the visitor ID has existing token Id
		Visitor visitor = Visitor.getUserWithVisitorId(visitorId);
		if(visitor == null){
			response.errorDescription = "Unknown visitor Id";
			return response;
		}
		
		/*Step 2: Based on the various submission perform actions
		  the various interactions could be
		  "submit" => User answers/submits response to question [Interaction 1]
		  "select" => User selects a challenge					[Interaction 2]
		  "done" => User session Done							[Interaction 3]
		  "startOver" => User session Start over				[Interaction 4]

		*/
		System.out.println(queryParams.getFirst(TYPE_PARAM));
		if(queryParams.containsKey(TYPE_PARAM)){
			String typeValue = (String) queryParams.getFirst(TYPE_PARAM);
			
			if(typeValue.equals(TYPE_SUBMIT)){
				//INTERACTION 1
				System.out.println("Received submit query");
				storeQuestionSubmission(visitorId, queryParams);
			}
			else if(typeValue.equals(TYPE_SELECT)){
				//INTERACTION 2
			}
			
		}
		
		//Step 3: Load the configuration information from the map.xml file
		String mapXMLFile = System.getenv("SLAP_MAP_XML_FILE");
		mapXMLFile = mapXMLFile == null || mapXMLFile.isEmpty() ? MAP_FILENAME : mapXMLFile;
		
		System.out.println("Loading configuration from " + mapXMLFile);
		ConfigurationMap configDetails = ConfigurationMap.getConfig(mapXMLFile);
		
		//Step 4: Prepare Server Query and fetch response from server
		//TODO: parallelize question and challenge response
		MultivaluedMap<String, String> questionParams = new MultivaluedHashMap<String, String>();
		questionParams.putAll(queryParams);
		questionParams.putSingle(SOURCE_PARAM, "questions");
		String questionResponse = new QueryManager().query(visitorId, questionParams, configDetails, "/questionresponse-1.xml");
		
		
		
		MultivaluedMap<String, String> challengeParams = new MultivaluedHashMap<String, String>();
		challengeParams.putAll(queryParams);
		challengeParams.putSingle(SOURCE_PARAM, "challenge");
		String challengeResponse = new QueryManager().query(visitorId, queryParams, configDetails,"/challengeresponse-1.xml");


		//Step 5: Merge the response and return the response
		
		response.questions = XmlParser.transformDoc(questionResponse, configDetails.backendDocNode, configDetails.responseMappings,"source|questions");
		response.items = XmlParser.transformDoc(challengeResponse, configDetails.backendDocNode, configDetails.responseMappings,"source|challenge");
		response.visitorId = visitorId;
		
		return response;
	}

	/**
	 * returns a response with visitor id for a given user id.
	 * @param userId the id of the user to be mapped to a visitor Id
	 * @return If a visitor id exists in  the db for the user, returns the value. Else creates a new mapping and returns that.
	 */
	@GET
	@Path("getvisitorid")
	@Produces({MediaType.APPLICATION_JSON})
	public Visitor getVisitorId(@QueryParam(value = "userid") String userId){
		Visitor visitor = new Visitor();
		if(userId != null && !userId.isEmpty()){
			try {
				visitor = Visitor.getVisitorFor(userId);
				if(visitor == null){
					visitor = Visitor.createVisitorFor(userId);
				}
			} catch (UnknownHostException e) {
				visitor.errorDescription = "Error occurred while connecting to the server";
			}
		}
		else{
			visitor.errorDescription = "Please provide a valid user id";
		}
		
		return visitor;
	}
	
	
	/**
	 * This method answers provided for a given question in the database for a given visitor Id
	 * @param visitorId the id of the visitor who provided the response
	 * @param queryParams the list of values provided.
	 * @throws UnknownHostException 
	 */
	private void storeQuestionSubmission(long visitorId, MultivaluedMap<String, String> queryParams) throws UnknownHostException{
		Question.storeQuestion(visitorId, queryParams);
	}
}
