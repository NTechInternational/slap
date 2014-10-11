package com.ntechinternational.slap;

import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.WriteResult;

public class Question {
	public long visitorId;
	public Map<String, String> questionResponses;
	
	
	/**
	 * stores the question response provided by the visitor, creates a new Question document if it exists
	 * else updates the question response.
	 * @param visitorId
	 * @throws UnknownHostException 
	 */
	public static void storeQuestion(long visitorId, MultivaluedMap<String, String> queryParams) throws UnknownHostException{
		
		System.out.println("Updating question information");
		
		DBCollection questions = Database.getCollection(Database.MONGO_QUESTION_COLLECTION_NAME);
		BasicDBObject updateQuery = new BasicDBObject();
	    updateQuery.put( "visitorId", visitorId );
	    
	    BasicDBList allParams = new BasicDBList();
	    
	    if(parseResponseStringAndGetResponses(allParams, queryParams)){
	    	System.out.println(allParams);
	    	
	    	for(int i = 0; i < allParams.size(); i++){
			    BasicDBObject updateCommand = new BasicDBObject();
			    updateCommand.put( "$push", new BasicDBObject("questions", allParams.get(i)) );
			    //TODO: insert all the question array in go using $pushAll or equivalent
			    WriteResult result = questions.update( updateQuery, updateCommand, true, true );
			    System.out.println("Write result: " + result);
	    	}
		    
	    }
	    
	}
	
	private static boolean parseResponseStringAndGetResponses(BasicDBList allParams, MultivaluedMap<String, String> queryParams){
		List<String> params = queryParams.get("questionResponse");
		
		if(params != null){
			for(String param : params){
				String[] responses = param.split("#");
				for(String response : responses){
					String[] nvp = response.split(":");
					if(nvp.length == 2){
						allParams.add(new BasicDBObject(nvp[0], nvp[1]));
					}
				}
			}
			
			return true;
		}
		
		return false;
	}
}
