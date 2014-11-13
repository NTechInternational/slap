package com.ntechinternational.slap;

import java.net.UnknownHostException;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;

public class Database {
	private static final String MONGO_DB_NAME = "slap";
	public static final String MONGO_VISITOR_COLLECTION_NAME = "visitors";
	public static final String MONGO_QUESTION_COLLECTION_NAME = "questions";
	public static final String MONGO_VISITOR_SESSION_COLLECTION_NAME = "visitorSession";
	public static final String MONGO_VISITOR_SESSION_START_OVER_COLLECTION_NAME = "visitorSessionStartOver";
	public static final String MONGO_TEMP_QUESTION_STORE = "tempQuestions";
	public static final String MONGO_TEMP_CHALLENGE_STORE = "tempChallenges";
	
	static volatile MongoClient _mongoClient = null;
	
	static String mongoAddress = "localhost";
	static int mongoPort = -1;
	

	/**
	 * initializes the mongo db client instance for the application, which is a pooled resource
	 * @param mongoAddress 
	 * @param mongoPort 
	 * @throws UnknownHostException
	 */
	private static void initialize() throws UnknownHostException{
		if(_mongoClient == null){
			synchronized(MongoClient.class){
				if(_mongoClient == null){
					//TODO: get the mongo server port from config file
					//for some reason when mongoPort is passed timeout occurs
					_mongoClient = new MongoClient(mongoAddress);
					DB slapDB = _mongoClient.getDB(MONGO_DB_NAME);
					
					DBCollection collection = slapDB.getCollection(MONGO_VISITOR_COLLECTION_NAME);
					//ensures that visitorId is unique
					BasicDBObject indexObj = new BasicDBObject("visitorId", 1).append("questionId", 1);
					BasicDBObject uniqueIndex = new BasicDBObject("unique", true);
					
					collection.createIndex(indexObj, uniqueIndex);
					
					//create question collection
					collection = slapDB.getCollection(MONGO_QUESTION_COLLECTION_NAME);
					collection.createIndex(indexObj, uniqueIndex);
					
					//create temp collection index
					collection = slapDB.getCollection(MONGO_TEMP_QUESTION_STORE);
					collection.createIndex(new BasicDBObject("visitorId", 1), uniqueIndex);
					
				}
			}
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
	
	public static void initializeMongoAddress(String mongoAddress, int mongoPort){
		if(mongoPort == -1){
			Database.mongoAddress = mongoAddress;
			Database.mongoPort = mongoPort;
		}
	}
	
	public static DBCollection getCollection(String collectionName) throws UnknownHostException {
		return Database.getDB().getCollection(collectionName);
	}
	
}
