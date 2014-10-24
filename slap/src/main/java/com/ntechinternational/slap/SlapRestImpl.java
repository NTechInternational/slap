package com.ntechinternational.slap;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.json.annotation.JacksonFeatures;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

@Path("/rest")
public class SlapRestImpl {
	
	private static final String TYPE_PARAM = "type";
	private static final String API_VERSION_PARAM = "apiversion";
	private static final String SOURCE_PARAM = "sourcetype";
	private static final String INVALID_VISITOR_ID_PROVIDED = "Invalid visitor ID provided";
	private static final String VISITOR_ID = "visitorId";
	private static final String MAP_FILENAME = "Map.xml";
	private static final String UNSUPPORTED_API_VERSION = "API Version unsupported, please provide correct apiversion parameter";
	
	enum Interaction { Submit, Select, Done, StartOver, Default };
	
	private String visitorId = null;
	
	/**
	 * This is the main web service method that processes all the various request and provides a response
	 * @return the string response
	 */
	@GET
	@Produces({MediaType.APPLICATION_JSON})
	@Path("processrequest")
	@JacksonFeatures(serializationEnable =  { SerializationFeature.INDENT_OUTPUT })
	public SlapResponse processRequest(@Context UriInfo uriInfo){
		
		SlapResponse processedResponse = new SlapResponse();;
		
		//if valid visitor id has been provided
		//TODO: check if the test is valid, and meets the requirements
		MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters(); 
		String visitorId = ""; //default null value
		int apiVersion = 0;
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
			
			if(apiVersion == 1){
				if(visitorId != null && !visitorId.isEmpty()){
					processedResponse = processRequest(queryParams);
				}
				else{
					processedResponse.errorDescription = INVALID_VISITOR_ID_PROVIDED;
				}
			}
			else{
				processedResponse.errorDescription = UNSUPPORTED_API_VERSION; 
			}
		}
		catch(Exception ex){
			System.err.println("An exception was occurred " + ex + " on ");
			ex.printStackTrace(System.err);
			processedResponse.errorDescription = ex.getMessage();
		}
		
		return processedResponse;
		
		
	}

	private SlapResponse processRequest(
		MultivaluedMap<String, String> queryParams) throws Exception {
		SlapResponse response = new SlapResponse();
		response.visitorId = visitorId;
		
		//Step 1: Load the configuration information from the map.xml file
		String mapXMLFile = System.getenv("SLAP_MAP_XML_FILE");
		mapXMLFile = mapXMLFile == null || mapXMLFile.isEmpty() ? MAP_FILENAME : mapXMLFile;
		
		System.out.println("Loading configuration from " + mapXMLFile);
		ConfigurationMap configDetails = ConfigurationMap.getConfig(mapXMLFile);
		System.out.println("Connecting to " + configDetails.mongoAddress + " @ " + configDetails.mongoPort);
		Database.initializeMongoAddress(configDetails.mongoAddress, configDetails.mongoPort);

		
		//Step 2: Validate the visitor ID has existing token Id
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
		
		switch(interactionType){
		case Select:
			selectInteraction(response, queryParams, configDetails);
			break;
		case Submit:
			submitInteraction(response, queryParams, configDetails);
			break;
		case Done:
			response.errorDescription = "Done interaction";
			break;
		case StartOver:
			resetAllInteraction(response, queryParams, configDetails);
			break;
		
		default:
			getResponseFromServer(response, queryParams, configDetails, null, null, new HashMap<String, String>());
			break;
		}
		
		
		return response;

	}
	
	private void resetAllInteraction(SlapResponse response,
			MultivaluedMap<String, String> queryParams,
			ConfigurationMap configDetails) throws Exception {
		
		BasicDBObject visitorQuery = new BasicDBObject("visitorId", this.visitorId);
		
		//remove from visitor map
		//Database.getCollection(Database.MONGO_VISITOR_COLLECTION_NAME).remove(visitorQuery);
		//clearing visitorId is not necessary
		
		
		Database.getCollection(Database.MONGO_QUESTION_COLLECTION_NAME).remove(visitorQuery);
		
		getResponseFromServer(response, queryParams, configDetails, null, null, new HashMap<String, String>());
		
	}

	private void selectInteraction(SlapResponse response,
			MultivaluedMap<String, String> queryParams,
			ConfigurationMap configDetails) throws Exception {

		String itemId = queryParams.getFirst("itemid");
		
		BasicDBObject visitorToUpdate = new BasicDBObject("visitorId", visitorId);
		BasicDBObject updateInfo = new BasicDBObject("$set", new BasicDBObject("selectedChallenge", 
										new BasicDBObject("itemId", itemId)));
		//TODO: store the challenge too.
		
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
			
			response.questions.addAll(XmlParser.transformDoc(questionResponse, configDetails.backendDocNode, configDetails.responseMappings,"source|questions"));
			
		}
	}

	/**
	 * Handler for submit interaction
	 * @throws Exception 
	 */
	private void submitInteraction(SlapResponse response, MultivaluedMap<String, String> queryParams, ConfigurationMap configDetails) throws Exception{
		String questionId = queryParams.getFirst("qid");
		String itemId = queryParams.getFirst("itemid");
		
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
					String camelcaseKey = key.substring(0, 1).toLowerCase() + key.substring(1);
					String backendKey = configDetails.requestMappings.get(camelcaseKey);
					if(backendKey == null)
						backendKey = key;
					additionalParams.putSingle("fq", backendKey + ":" + f.get(key) );
				}
			}
			
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
				
				response.questions.addAll(XmlParser.transformDoc(questionResponse, configDetails.backendDocNode, configDetails.responseMappings,"source|questions"));
				
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
		
		System.out.println("Challenge Response: " + challengeResponse);
		List<Map<String, Object>> challenges = XmlParser.transformDoc(challengeResponse, configDetails.backendDocNode, configDetails.responseMappings,"source|challenge");
		if(challenges.size() == 1){
			Template substitute = new Template(challenges.get(0).get("itemtemplate").toString());
			
			substitute.process(respondedVariables, VariableUtility.getVariablesFromString(challenges.get(0).get("variables").toString()));
			
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
				
				System.out.println("Variable query : "  + variableQuery);
				
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
						.process(respondedValues, VariableUtility.getVariablesFromString(challenge.get("variables").toString())));
						//just providing
		}
		
		response.items = substituteVariables(challenges);
		response.visitorId = visitorId;
		
	}

	/**
	 * transforms the question's answer from pipe separated value to JSON objects
	 */
	private void changeSchemaForAnswers(List<Map<String, Object>> questions) {
		
		
		for(Map<String,Object> question : questions){
			System.out.println(question);
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
				
				variablesToStore.add(new BasicDBObject(keyVal[0], keyVal[1]));
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

	/**
	 * Substitutes all the variables in the challenge and returns the new challenge
	 * @return
	 * @throws UnknownHostException 
	 */
	private List<Map<String, Object>> substituteVariables(List<Map<String, Object>> items) throws UnknownHostException{
		BasicDBObject query = new BasicDBObject("visitorId", visitorId);
		DBCursor cursor = Database.getCollection(Database.MONGO_QUESTION_COLLECTION_NAME).find(query);
		boolean wasReplaced = false;
		
		List<StringBuilder> allItemTemplates = new ArrayList<StringBuilder>();
		for(Map<String, Object> item : items){
			String itemTemplate = (String) item.get("itemtemplate");
			
			allItemTemplates.add(new StringBuilder(itemTemplate));
		}
		
		
		
		
		//find all the responded variables and use them to replace all text in items
		while(cursor.hasNext()){
			wasReplaced = true;
			DBObject storedResponse = cursor.next(); //get the response for a question
			BasicDBList variables = (BasicDBList)storedResponse.get("variables");
			
			for(int index = 0; index < variables.size(); index++){
				DBObject variable = (DBObject)variables.get(index);
				String key = variable.keySet().iterator().next();
				String value = (String)variable.get(key);
				
				key = "&" + key;
				
				for(StringBuilder itemTemplate : allItemTemplates){
					
					replaceAll(itemTemplate, key, value);
					System.out.println(itemTemplate);
				}
				
			}
		}
		
		//rewrite the string in the param object
		if(wasReplaced){
			int index = 0;
			for(Map<String, Object> item : items){
				
				item.put("itemtemplate", allItemTemplates.get(index).toString());
				
				index++;
			}
		}
		
		return items;
	}
	
	private void replaceAll(StringBuilder builder, String from, String to)
	{
	    int index = builder.indexOf(from);
	    while (index != -1)
	    {
	        builder.replace(index, index + from.length(), to);
	        index += to.length(); // Move to the end of the replacement
	        index = builder.indexOf(from, index);
	    }
	}


}
