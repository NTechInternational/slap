package com.ntechinternational.slap;

import java.net.UnknownHostException;

import javax.ws.rs.core.MultivaluedMap;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

public class TokenValidator {
	
	private static final String MONGO_DB_NAME = "slap";
	private static final String MONGO_COLLECTION_NAME = "visitors";
	static MongoClient _mongoClient = null;
	

	/**
	 * initializes the mongo db client instance for the application, which is a pooled resource
	 * Since mongoclient  has 
	 * @throws UnknownHostException
	 */
	private synchronized static void initialize() throws UnknownHostException{
		if(_mongoClient == null){
			//TODO: get the mongo server name and port from config file
			_mongoClient = new MongoClient();
			DB slapDB = _mongoClient.getDB(MONGO_DB_NAME);
			
			DBCollection collection = slapDB.getCollection(MONGO_COLLECTION_NAME);
			//ensures that visitorId is unique
			BasicDBObject indexObj = new BasicDBObject("visitorId", 1);
			BasicDBObject uniqueIndex = new BasicDBObject("unique", true);
			
			collection.createIndex(indexObj, uniqueIndex);
		}
		
	}
	
	/**
	 * returns an instance of a DB connection for the mongo client.
	 * @return
	 * @throws UnknownHostException
	 */
	private static DB getDB() throws UnknownHostException{
		initialize();
		return _mongoClient.getDB(MONGO_DB_NAME);
	}

	/**
	 * This method checks if a valid token Id exists for a visitorId, if it does returns the token, else
	 * creates one and returns the token
	 * @param visitorId
	 * @return Token mapped to the visitor Id
	 * @throws UnknownHostException 
	 */
	public Token checkTokenId(long visitorId, MultivaluedMap<String, String> params) throws UnknownHostException{
		Token token = new Token();
		
		DB slapDB = TokenValidator.getDB();
		DBCollection visitors = slapDB.getCollection(MONGO_COLLECTION_NAME);
		
		//Check if visitor exists in the db
		DBObject visitor = visitors.findOne(new BasicDBObject("visitorId", visitorId));
		
		//if one is not found
		if(visitor == null){
			System.out.println("VisitorID missing in the db, adding one");
			
			//insert one
			visitor = new BasicDBObject("visitorId", visitorId)
							.append("tokenId", "RAND-123");
			
			visitors.insert(visitor);
		}
		
		//return the token Id
		token.tokenId = (String)visitor.get("tokenId");
		
		return token;
	}
}
