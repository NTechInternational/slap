package com.ntechinternational.slap;

import java.net.UnknownHostException;

import javax.ws.rs.core.MultivaluedMap;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

public class Question {
	public DBObject questionObj;
	
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
			    //questions.find()
			    System.out.println("Write result: " + result);
	    	}
		    
	    }
	    
	}
	
	private static boolean parseResponseStringAndGetResponses(BasicDBList allParams, MultivaluedMap<String, String> queryParams){
		 //&qid=2001&label=customer name&value=customer&qtype=variable&..&visitorId=123&type=submit
		if(queryParams.getFirst("qid") != null){
			BasicDBObject obj = new BasicDBObject("qid", queryParams.getFirst("qid"));
			
			
			obj.append("label", queryParams.get("label"))
			   .append("value", queryParams.get("value"))
			   .append("qtype", queryParams.get("qtype"));
			
			allParams.add(obj);
			
			return true;
		}
		
		return false;
	}
}
