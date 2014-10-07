package com.ntechinternational.slap;

import java.net.UnknownHostException;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;

public class Database {
	private static final String MONGO_DB_NAME = "slap";
	public static final String MONGO_VISITOR_COLLECTION_NAME = "visitors";
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
			
			DBCollection collection = slapDB.getCollection(MONGO_VISITOR_COLLECTION_NAME);
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
	
	public static DBCollection getCollection(String collectionName) throws UnknownHostException {
		
		return Database.getDB().getCollection(collectionName);
	}
}
