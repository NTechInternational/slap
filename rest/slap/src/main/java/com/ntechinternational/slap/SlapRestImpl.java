package com.ntechinternational.slap;

import java.io.IOException;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.apache.logging.log4j.LogManager;

import threescale.v3.api.AuthorizeResponse;
import threescale.v3.api.ParameterMap;
import threescale.v3.api.ServerError;
import threescale.v3.api.ServiceApi;
import threescale.v3.api.impl.ServiceApiDriver;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.ntechinternational.slap.ConfigurationMap;
import com.ntechinternational.slap.LogUtil;

@Path("/rest")
public class SlapRestImpl {
	
	private static final String PARAM_ITEM_ID = "itemId";
	private static final String TYPE_PARAM = "type";
	private static final String API_VERSION_PARAM = "apiversion";
	private static final String SOURCE_PARAM = "sourcetype";
	private static final String INVALID_VISITOR_ID_PROVIDED = "Invalid visitor ID provided";
	private static final String VISITOR_ID = "visitorId";
	private static final String MAP_FILENAME = "Map.xml";
	private static final String UNSUPPORTED_API_VERSION = "API Version unsupported, please provide correct apiversion parameter";
	
	enum Interaction { Submit, Select, Done, StartOver, Default };
	
	private String visitorId = null;
	private ConfigurationMap configDetails = null;
	
	/**
	 * This is the main web service method that processes all the various request and provides a response
	 * @return the string response
	 */
	@GET
	@Path("processrequest")
	public Response processRequest(@Context UriInfo uriInfo){
		
		SlapResponse processedResponse = new SlapResponse();
		final MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
		String visitorId = ""; //default null value
		int apiVersion = 0;
		
		LogUtil.debug("Received a process request");
		
		//if valid visitor id has been provided
		try{
			visitorId =  queryParams.getFirst(VISITOR_ID);
			processedResponse.visitorId = visitorId;
			this.visitorId = visitorId;
			
			//check the api version number is supported
			try{
				apiVersion = Integer.parseInt(queryParams.getFirst(API_VERSION_PARAM));
			}
			catch(NumberFormatException invalidVersion){
				apiVersion = 1; //set to current version level
			}
			
			LogUtil.trace("Using API Version" + apiVersion);
			
			if(apiVersion == 1){
				if(visitorId != null && !visitorId.isEmpty()){
					processedResponse = processRequest(queryParams);
				}
				else{
					processedResponse.errorDescription = INVALID_VISITOR_ID_PROVIDED;
				}
			}
			else{
				LogUtil.error("Unsupported api version " + apiVersion + " requested");
				processedResponse.errorDescription = UNSUPPORTED_API_VERSION; 
			}
		}
		catch(Exception ex){
			LogUtil.error("An exception was occurred " + ex + " on ");
			ex.printStackTrace(System.err);
			processedResponse.errorDescription = ex.getMessage();
		}
		
		
		return createResponse(processedResponse, queryParams).build();
		
	}

	private SlapResponse processRequest(
		MultivaluedMap<String, String> queryParams) throws Exception {
		SlapResponse response = new SlapResponse();
		response.visitorId = visitorId;
		
		
		//Step 1: Load the configuration information from the map.xml file
		String mapXMLFile = System.getenv("SLAP_MAP_XML_FILE");
		mapXMLFile = mapXMLFile == null || mapXMLFile.isEmpty() ? MAP_FILENAME : mapXMLFile;
		
		LogManager.getRootLogger().debug("Loading configuration from " + mapXMLFile);
		
		this.configDetails = ConfigurationMap.getConfig(mapXMLFile);
		LogUtil.debug("Connecting to " + configDetails.mongoAddress + " @ " + configDetails.mongoPort);
		Database.initializeMongoAddress(configDetails.mongoAddress, configDetails.mongoPort);

		//Step 2.1 Validate with 3Scale
		if(!threeScaleAuth(queryParams, response))
			return response;
		
		//Step 2.2: Validate the visitor ID has existing token Id
		Visitor visitor = Visitor.getUserWithVisitorId(visitorId);
		if(visitor == null){
			response.errorDescription = "Unknown visitor Id";
			return response;
		}
		
		/*Step 3: Based on the various submission perform actions
		  the various interactions could be
		   &qid=2001&label=customer name&value=customer&qtype=variable&..&visitorId=123&type=submit
		  "submit" => User answers/submits response to question [Interaction 1]
		  "select" => User selects a challenge					[Interaction 2]
		  "done" => User session Done							[Interaction 3]
		  "startOver" => User session Start over				[Interaction 4]

		*/
		
		Interaction interactionType = Interaction.Default;
		String queryInteractionType = queryParams.getFirst(TYPE_PARAM);
		if(queryInteractionType != null && !queryInteractionType.isEmpty()){
			try{
				
				queryInteractionType = queryInteractionType.substring(0, 1).toUpperCase() + queryInteractionType.substring(1);
				interactionType = Interaction.valueOf(queryInteractionType);
			}
			catch(IllegalArgumentException ex){
				//safely ignoring illegal argument exception
				response.errorDescription = "Invalid interaction type submitted " + queryInteractionType;
				return response;
			}
		}
		
		LogUtil.debug("Selected interaction is " + interactionType);
		switch(interactionType){
		case Select:
			selectInteraction(response, queryParams, configDetails, queryParams.getFirst(PARAM_ITEM_ID));
			break;
		case Submit:
			submitInteraction(response, queryParams, configDetails);
			break;
		case Done:
			doneInteraction(response, queryParams, configDetails);
			break;
		case StartOver:
			resetAllInteraction(response, queryParams, configDetails);
			break;
		
		default:
			defaultInteraction(response, queryParams, configDetails);
			break;
		}
		
		return response;

	}
	
	private void defaultInteraction(SlapResponse response,
			MultivaluedMap<String, String> queryParams,
			ConfigurationMap configDetails) throws Exception {
		
		BasicDBObject query = new BasicDBObject("visitorId", visitorId);
		
		DBObject visitorInfo = Database.getCollection(Database.MONGO_VISITOR_COLLECTION_NAME).findOne(query);
		DBObject previousChallenge = (DBObject)visitorInfo.get("selectedChallenge");
		
		if(previousChallenge != null && previousChallenge.get("itemId") != null){
			String previouslySelectedItemId = (String) previousChallenge.get("itemId");
			selectInteraction(response, queryParams, configDetails, previouslySelectedItemId);
			return;
		}
		
		DBCursor allQuestions = Database.getCollection(Database.MONGO_QUESTION_COLLECTION_NAME).find(query);
		
		
		
		//append all facets provided
		MultivaluedMap<String, String> additionalParams = new MultivaluedHashMap<String, String>();
		
		
		while(allQuestions.hasNext()){
			DBObject question = allQuestions.next();
			BasicDBList facets = (BasicDBList)question.get("facets");
			if(facets != null){
				for(Object facet: facets){
					DBObject f = (DBObject)facet;
					String key = f.keySet().iterator().next();
					String lowercaseKey = key.toLowerCase();
					String backendKey = configDetails.requestMappings.get(lowercaseKey);
					if(backendKey == null)
						backendKey = key;
					additionalParams.add("fq", backendKey + ":" + f.get(key) );
				}
			}
		}
		
		if(additionalParams.size() == 0){
			additionalParams.putSingle("fq", configDetails.requestMappings.get("businessmodel") + ":All");
		}
		
		additionalParams.putSingle("rows", "1");
		
		getResponseFromServer(response, queryParams, configDetails, additionalParams, null, new HashMap<String, String>());
		
	}

	private void doneInteraction(SlapResponse response,
			MultivaluedMap<String, String> queryParams,
			ConfigurationMap configDetails) throws Exception {

		if(queryParams.containsKey("qid") && (queryParams.containsKey("var") || queryParams.containsKey("facet"))){
			//this is similar to submit interaction so save the submitted values
			submitInteraction(response, queryParams, configDetails);
		}
		else{
			defaultInteraction(response, queryParams, configDetails);
		}

		
		//save all the interaction responses to visitor session collection
		BasicDBObject query = new BasicDBObject("visitorId", this.visitorId);
		
		DBObject visitorInfo = Database.getCollection(Database.MONGO_VISITOR_COLLECTION_NAME).findOne(query);
		
		DBCursor allQuestions = Database.getCollection(Database.MONGO_QUESTION_COLLECTION_NAME).find(query);
		
		BasicDBObject objectToSave = new BasicDBObject("visitorId", this.visitorId).append("sessionCompletedOn",new Date());
		objectToSave.append("selectedChallenge", visitorInfo.get("selectedChallenge"));
		BasicDBList questions = new BasicDBList();
		
		
		while(allQuestions.hasNext()){
			DBObject question = allQuestions.next();
			question.removeField("visitorId"); //remove visitor id from the object because it is already present in the parent object
			questions.add(question);
		}
		
		objectToSave.append("questions", questions);
		
		Database.getCollection(Database.MONGO_VISITOR_SESSION_COLLECTION_NAME).insert(objectToSave);
	}

	private void resetAllInteraction(SlapResponse response,
			MultivaluedMap<String, String> queryParams,
			ConfigurationMap configDetails) throws Exception {
		
		BasicDBObject visitorQuery = new BasicDBObject("visitorId", this.visitorId);
		
		//remove from visitor map
		//Database.getCollection(Database.MONGO_VISITOR_COLLECTION_NAME).remove(visitorQuery);
		//clearing visitorId is not necessary
		
		
		Database.getCollection(Database.MONGO_QUESTION_COLLECTION_NAME).remove(visitorQuery);
		
		BasicDBObject updateInfo = new BasicDBObject("$set", new BasicDBObject("selectedChallenge", 
										null ));
		
		Database.getCollection(Database.MONGO_VISITOR_COLLECTION_NAME).update(visitorQuery, updateInfo);
		
		defaultInteraction(response, queryParams, configDetails);
		
	}

	private void selectInteraction(SlapResponse response,
			MultivaluedMap<String, String> queryParams,
			ConfigurationMap configDetails, String itemId) throws Exception {

		
		BasicDBObject visitorToUpdate = new BasicDBObject("visitorId", visitorId);
		BasicDBObject updateInfo = new BasicDBObject("$set", new BasicDBObject("selectedChallenge", 
										new BasicDBObject(PARAM_ITEM_ID, itemId)));
		
		
		Database.getCollection(Database.MONGO_VISITOR_COLLECTION_NAME).update(visitorToUpdate, updateInfo);
		
		
		BasicDBObject query = new BasicDBObject("visitorId", visitorId);
		DBCursor allQuestions = Database.getCollection(Database.MONGO_QUESTION_COLLECTION_NAME).find(query);
		
		Map<String, String> respondedVariables = new HashMap<String, String>();
		
		while(allQuestions.hasNext()){
			DBObject question = allQuestions.next();
			
			BasicDBList variables = (BasicDBList)question.get("variables");
			if(variables != null){
				for(Object variable: variables){
					DBObject f = (DBObject)variable;
					String key = f.keySet().iterator().next();
					respondedVariables.put(key, f.get(key).toString());
				}
			}
		}
		
		MultivaluedMap<String, String> additionalParams = new MultivaluedHashMap<String, String>();
		MultivaluedMap<String, String> additionalParamsWithDefaults = new MultivaluedHashMap<String, String>();
		findMissingVariables(queryParams, configDetails, itemId, additionalParams, additionalParamsWithDefaults, respondedVariables);
		
		MultivaluedMap<String, String> additionalParamsChallenge = new MultivaluedHashMap<String, String>();
		additionalParamsChallenge.putSingle("fq", "id:" + itemId);
		
		
		getResponseFromServer(response, queryParams, configDetails, additionalParams, additionalParamsChallenge, respondedVariables);

		//if default variable values haven't been provided
		if(additionalParamsWithDefaults.size() > 0){
			MultivaluedMap<String, String> questionParams = new MultivaluedHashMap<String, String>();
			questionParams.putAll(queryParams);
			questionParams.putSingle(SOURCE_PARAM, "questions");
			String questionResponse = new QueryManager().query(visitorId, questionParams, configDetails, configDetails.questionPath, additionalParamsWithDefaults);
			

			List<Map<String, Object>> questions = XmlParser.transformDoc(questionResponse, configDetails.backendDocNode, configDetails.responseMappings,"source|questions");
			changeSchemaForAnswers(questions);
			response.questions.addAll(questions);
			
		}
	}

	/**
	 * Handler for submit interaction
	 * @throws Exception 
	 */
	private void submitInteraction(SlapResponse response, MultivaluedMap<String, String> queryParams, ConfigurationMap configDetails) throws Exception{
		String questionId = queryParams.getFirst("qid");
		String itemId = queryParams.getFirst(PARAM_ITEM_ID);
		
		if(itemId == null){
			BasicDBObject visitorToUpdate = new BasicDBObject("visitorId", visitorId);
			//get a previously selected challenge
			DBObject visitor = Database.getCollection(Database.MONGO_VISITOR_COLLECTION_NAME).findOne(visitorToUpdate);
			if(visitor != null && visitor.containsField("selectedChallenge.itemId")){
				itemId = (String) visitor.get("selectedChallenge.itemId");
			}
			
		}
		
		MultivaluedMap<String, String> additionalParamsChallenge = new MultivaluedHashMap<String, String>();

		if(itemId != null){
			additionalParamsChallenge.putSingle("fq", "id:" + itemId);
		}
		
		storeQuestionSubmission(questionId, queryParams);
		
		
		BasicDBObject query = new BasicDBObject("visitorId", visitorId);
		DBCursor allQuestions = Database.getCollection(Database.MONGO_QUESTION_COLLECTION_NAME).find(query);
		
		//append all facets provided
		MultivaluedMap<String, String> additionalParams = new MultivaluedHashMap<String, String>();
		MultivaluedMap<String, String> additionalParamsWithDefaultsOnly = new MultivaluedHashMap<String, String>();
		
		Map<String, String> respondedVariables = new HashMap<String, String>();
		
		while(allQuestions.hasNext()){
			DBObject question = allQuestions.next();
			
			BasicDBList facets = (BasicDBList)question.get("facets");
			if(facets != null){
				for(Object facet: facets){
					DBObject f = (DBObject)facet;
					String key = f.keySet().iterator().next();
					String camelcaseKey = key.toLowerCase();
					String backendKey = configDetails.requestMappings.get(camelcaseKey);
					if(backendKey == null)
						backendKey = key;
					additionalParams.add("fq", backendKey + ":" + f.get(key) );
				}
			}
			
			LogUtil.trace("Additional Param to be sent to server: " + additionalParams);
			
			BasicDBList variables = (BasicDBList)question.get("variables");
			if(variables != null){
				for(Object variable: variables){
					DBObject f = (DBObject)variable;
					String key = f.keySet().iterator().next();
					respondedVariables.put(key, f.get(key).toString());
				}
			}

		}
		
		
		//if item id is not null, that is a challenge has been selected
		if(itemId != null && itemId.isEmpty() == false){
			additionalParams.clear(); //we are not using the facet query but variable query
			
			findMissingVariables(queryParams, configDetails, itemId,
					additionalParams, additionalParamsWithDefaultsOnly, respondedVariables);
		}
		
		
		getResponseFromServer(response, queryParams, configDetails, additionalParams, additionalParamsChallenge, respondedVariables);
		
		//return only one question when facet was submitted for a question
		if(queryParams.getFirst("itemid") == null && 
				queryParams.getFirst("qid") != null &&
				queryParams.getFirst("facet") != null){
			response.questions = response.questions.size() > 0 ? response.questions.subList(0, 1) : response.questions;
		}
		else{
			if(additionalParamsWithDefaultsOnly.size() > 0){
				MultivaluedMap<String, String> questionParams = new MultivaluedHashMap<String, String>();
				questionParams.putAll(queryParams);
				questionParams.putSingle(SOURCE_PARAM, "questions");
				String questionResponse = new QueryManager().query(visitorId, questionParams, configDetails, configDetails.questionPath, additionalParamsWithDefaultsOnly);
				
				List<Map<String, Object>> questions = XmlParser.transformDoc(questionResponse, configDetails.backendDocNode, configDetails.responseMappings,"source|questions");
				changeSchemaForAnswers(questions);
				response.questions.addAll(questions);
				
			}
		}
	}

	/**
	 * This method finds the missing variables in the challenge.
	 */
	private void findMissingVariables(
			MultivaluedMap<String, String> queryParams,
			ConfigurationMap configDetails, String itemId,
			MultivaluedMap<String, String> missingParams,
			MultivaluedMap<String, String> missingParamsWithDefaultValueOnly,
			Map<String, String> respondedVariables) throws Exception {
		
		MultivaluedMap<String, String> paramsToAdd = new MultivaluedHashMap<String, String>();
		paramsToAdd.putSingle("fq", "id:" + itemId);
		String challengeResponse = new QueryManager().query(visitorId, queryParams, configDetails, configDetails.challengePath, paramsToAdd);
		
		LogUtil.trace("Challenge Response: " + challengeResponse);
		List<Map<String, Object>> challenges = XmlParser.transformDoc(challengeResponse, configDetails.backendDocNode, configDetails.responseMappings,"source|challenge");
		if(challenges.size() == 1){
			Template substitute = new Template(challenges.get(0).get("itemtemplate").toString());
			
			substitute.process(respondedVariables, Template.getVariablesFromString(challenges.get(0).get("variables").toString()));
			
			//check if any variable value is missing
			
			if(substitute.variablesWithoutValue.size() > 0){
				StringBuilder variableQuery = new StringBuilder();
				variableQuery.append("Variables_s:(");
				for(int index = 0, length = substitute.variablesWithoutValue.size(); index < length; index++){
					String variable = substitute.variablesWithoutValue.get(index);								
					variableQuery.append(variable);
					if(index+1 != length){
						//is not the last query append
						variableQuery.append(" OR ");
					}
				}
				variableQuery.append(")");
				
				LogUtil.trace("Variable query : "  + variableQuery);
				
				missingParams.putSingle("fq", variableQuery.toString());
				
				StringBuilder variableWithoutValueQuery = new StringBuilder();
				variableWithoutValueQuery.append("Variables_s:(");
				for(int index = 0, length = substitute.variablesWithDefaultValueOnly.size(); index < length; index++){
					String variable = substitute.variablesWithDefaultValueOnly.get(index);								
					variableWithoutValueQuery.append(variable);
					if(index+1 != length){
						//is not the last query append
						variableWithoutValueQuery.append(" OR ");
					}
				}
				variableWithoutValueQuery.append(")");
				
				missingParamsWithDefaultValueOnly.putSingle("fq", variableWithoutValueQuery.toString());
			}
		}
	}

	/**
	 * This method performs the default interaction with the server and sets the slap response with correct values.
	 */
	private void getResponseFromServer(SlapResponse response, MultivaluedMap<String, String> queryParams, 
			ConfigurationMap configDetails, MultivaluedMap<String, String> paramsToSubmitQuestion,
			MultivaluedMap<String, String> paramsToSubmitItem,
			Map<String, String> respondedValues)
	throws Exception{
		
		MultivaluedMap<String, String> questionParams = new MultivaluedHashMap<String, String>();
		questionParams.putAll(queryParams);
		questionParams.putSingle(SOURCE_PARAM, "questions");
		String questionResponse = new QueryManager().query(visitorId, questionParams, configDetails, configDetails.questionPath, paramsToSubmitQuestion);
		
		
		
		MultivaluedMap<String, String> challengeParams = new MultivaluedHashMap<String, String>();
		challengeParams.putAll(queryParams);
		challengeParams.putSingle(SOURCE_PARAM, "challenge");
		String challengeResponse = new QueryManager().query(visitorId, queryParams, configDetails, configDetails.challengePath, paramsToSubmitItem );


		//Step 4: Merge the response and return the response
		
		response.questions = XmlParser.transformDoc(questionResponse, configDetails.backendDocNode, configDetails.responseMappings,"source|questions"); 
		List<Map<String, Object>> challenges = XmlParser.transformDoc(challengeResponse, configDetails.backendDocNode, configDetails.responseMappings,"source|challenge");
		
		changeSchemaForAnswers(response.questions); //transforms the schema of answers to JSON object
		
		for(Map<String, Object> challenge : challenges){
			challenge.put("itemtemplate", 
					new Template(challenge.get("itemtemplate").toString())
						.process(respondedValues, Template.getVariablesFromString(challenge.get("variables").toString())));
						//just providing
		}
		
		response.items = new Substitutor().substituteVariables(challenges, visitorId);
		response.visitorId = visitorId;
		
	}

	/**
	 * transforms the question's answer from pipe separated value to JSON objects
	 */
	private void changeSchemaForAnswers(List<Map<String, Object>> questions) {
		for(Map<String,Object> question : questions){
			
			String answer = question.get("answers").toString();
			List<Map<String, String>> allOptions = new ArrayList<Map<String, String>>();
			String[] options = answer.split("\\|"); // all options are separated by | symbol
			for(String option : options){
				Map<String, String> objOption = new HashMap<String, String>();; //objOption represents the JSON object that will be returned.
				String[] values = option.split(","); //Each option describes Choice text, value and facet separated by ,
				
				for(String value : values){
					String[] keyValuePair = value.split(":"); //Each value is separated by :
					
					if(keyValuePair.length == 2){
						objOption.put(keyValuePair[0].trim(), keyValuePair[1].trim());
					}
				}
				
				if(objOption.size() > 0){
					allOptions.add(objOption);
				}
			}
			
			question.put("answers", allOptions); //replace the answer with formatted answer option
		}
		
	}

	/**
	 * returns a response with visitor id for a given user id.
	 * @param userId the id of the user to be mapped to a visitor Id
	 * @return If a visitor id exists in  the db for the user, returns the value. Else creates a new mapping and returns that.
	 */
	@GET
	@Path("getvisitorid")
	public Response getVisitorId(@Context UriInfo uriInfo,
		@QueryParam(value = "userid") String userId){
		final MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
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
		
		return createResponse(visitor, queryParams).build();
	}

	/**
	* wraps a response to jsonp if required
	*/
	private Response.ResponseBuilder createResponse(Object objectToWrap, final MultivaluedMap<String, String> queryParams){
		final Object objectToOutput = objectToWrap; //creating a final variable to pass to anonymous inner class
		// This code serializes the actual response
		StreamingOutput output = new StreamingOutput() {
			
			public void write(OutputStream outputStream) throws IOException,
					WebApplicationException {
				
				ObjectMapper mapper = new ObjectMapper();
				
				
				String callback = queryParams.getFirst("callback");
				
				//if pretty param is present prettifies the output
				if(queryParams.containsKey("pretty"))
					mapper.enable(SerializationFeature.INDENT_OUTPUT);
				
				mapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
				
				//if callback parameter is passed as a URL parameter
				//A JSONP response callback(output) is called
				//else a simple JSON object is returned
				if(callback == null || callback.isEmpty()){
					mapper.writeValue(outputStream, objectToOutput);
				}
				else{
					outputStream.write( (callback + "(").getBytes());
					mapper.writeValue(outputStream, objectToOutput);
					outputStream.write(");".getBytes());
					outputStream.flush();
					outputStream.close();
				}
				
			}
			
		};
		
		
		
		String returnType = queryParams.getFirst("callback") != null ? "application/javascript" : MediaType.APPLICATION_JSON;

		return Response.ok(output, returnType);
	}
	
	
	/**
	 * This method answers provided for a given question in the database for a given visitor Id
	 * @param visitorId the id of the visitor who provided the response
	 * @param queryParams the list of values provided.
	 * @throws UnknownHostException 
	 */
	private SlapResponse storeQuestionSubmission( String questionId, MultivaluedMap<String, String> queryParams) throws UnknownHostException{
		
		//for each question submission store the variable and facets 
		//and return the next question
		//Query String would contain
		//var=<Variable Name>:<Variable value>&var=<Variable Name>:<Variable value>...
		//Similarly facet would be contained in
		//facet=<Facet Name>:<Facet value>&facet=<Facet Name>:<Facet value>...
		List<String> variables = queryParams.get("var"),
					facets = queryParams.get("facet");
		
		//split all variables and facet and store them in db
		//all variables and facet are returned as <variable name>:<variable value>
		
		BasicDBList variablesToStore = new BasicDBList();
		if(variables != null){
			for(String variable : variables){
				String[] keyVal = variable.split(":"); //split by : to get key and value
				
				if(keyVal.length != 2)
					continue; //TODO: to decide whether to ignore if key val combination is not provided or to give an error
				
				variablesToStore.add(new BasicDBObject("&".concat(keyVal[0]), keyVal[1]));
			}
		}
		
		BasicDBList facetsToStore = new BasicDBList();
		if(facets != null){
			for(String facet : facets){
				String[] keyVal = facet.split(":"); //split by : to get key and value
				
				if(keyVal.length != 2)
					continue; //TODO: to decide whether to ignore if key val combination is not provided or to give an error
				
				facetsToStore.add(new BasicDBObject(keyVal[0], keyVal[1]));
			}
		}
		
		BasicDBObject objectToStore = new BasicDBObject("visitorId", visitorId).append("questionId", questionId);
		objectToStore.append("variables", variablesToStore)
					 .append("facets", facetsToStore);
		
		BasicDBObject query = new BasicDBObject("visitorId", visitorId).append("questionId", questionId);
		
		Database.getCollection(Database.MONGO_QUESTION_COLLECTION_NAME).update(query, objectToStore, true, false);
		
		SlapResponse response = new SlapResponse();
		response.visitorId = visitorId;
		response.questions = new ArrayList<Map<String, Object>>();
		
		//query = new BasicDBObject("visitorId", visitorId);
		//DBObject cachedEntity = Database.getCollection(Database.MONGO_TEMP_QUESTION_STORE).findOne(query);
		//Map<String, Object> nextQuestion = getNextQuestion(questionId, (List<Map<String, Object>>)cachedEntity.get("questions"));
		
		//if next question is null it is time to fetch questions from server
		//response.questions.add(nextQuestion);
		//response.items = substituteVariables((List<Map<String, Object>>)cachedEntity.get("challenges"));
		
		
		
		return response;
		//Question.storeQuestion(visitorId, queryParams);
	}
	
	/*private Map<String, Object> getNextQuestion(String questionId,List<Map<String, Object>> questions) throws UnknownHostException {
		
		
		for(int index = 0, length = questions.size(); index < length; index++){
			Map<String, Object> question = questions.get(index);
			System.out.println("Checking question "  +question.get("id"));
			if(question.get("id").equals(questionId)){
				
				if(index + 1 < length)
					return questions.get(index+1);
			}
			
		}
	
		return null;
	}*/

	private boolean threeScaleAuth(MultivaluedMap<String, String> queryParams, SlapResponse responseToReturn){
		
		if(configDetails.threeScaleEnabled.equalsIgnoreCase("off")){
			LogUtil.debug("3 scale is turned off");
			return true;
		}
		
		LogUtil.debug("Authenticating with 3Scale");
		
		ServiceApi serviceApi = new ServiceApiDriver(configDetails.threeScaleProviderKey);
		
		String appId = queryParams.getFirst("appId"),
				appKey = queryParams.getFirst("appKey");
		
		appId = appId == null ? configDetails.threeScaleAppId : appId;
		appKey = appKey == null ? configDetails.threeScaleAppKey : appKey;
		
		ParameterMap params = new ParameterMap();      // the parameters of your call
		params.add("service_id", appId);  // Add the service id of your application
		params.add("user_key", appKey);
		
		ParameterMap usage = new ParameterMap(); // Add a metric to the call
		usage.add("hits", "1");
		params.add("usage", usage);              // metrics belong inside the usage parameter
		
		AuthorizeResponse response = null;
		// the 'preferred way' of calling the backend: authrep
		try {
			response = serviceApi.authrep(params);
			LogUtil.debug("AuthRep on User Key Success: " + response.success());
			
			if (response.success() == true) {
				// your api access got authorized and the  traffic added to 3scale backend
				LogUtil.trace("Plan: " + response.getPlan());
			} else {
				// your api access did not authorized, check why
				LogUtil.trace("Error: " + response.getErrorCode());
				LogUtil.trace("Reason: " + response.getReason());
				
				responseToReturn.errorDescription = response.getReason();
				
			}
		} catch (ServerError serverError) {
			LogUtil.error(serverError.getMessage());
			return false;
		}
		
		return response.success();
	}
}
