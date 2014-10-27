package com.ntechinternational.slap;

import java.net.UnknownHostException;
import java.security.SecureRandom;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

@XmlRootElement
public class Visitor {
	
	private static SecureRandom random = new SecureRandom();

	@XmlElement
	public String userId;
	
	@XmlElement
	public String visitorId;
	
	@XmlElement
	@JsonInclude(Include.NON_NULL)
	public String errorDescription;

	/**
	 * Queries the mongo db object for visitor information given a user id.
	 * @param userId
	 * @return the userId to visitorId information
	 * @throws UnknownHostException
	 */
	public static Visitor getVisitorFor(String userId) throws UnknownHostException{
		
		DBCollection collection = Database.getCollection(Database.MONGO_VISITOR_COLLECTION_NAME);
		DBObject visitor = collection.findOne(new BasicDBObject("userId", userId));

		return visitor == null ? null : new Visitor(userId, (String)visitor.get("visitorId"));
		
	}
	
	/**
	 * creates a visitor for a given user id
	 * @param userId
	 * @return
	 * @throws UnknownHostException 
	 */
	public static Visitor createVisitorFor(String userId) throws UnknownHostException{
		DBCollection collection = Database.getCollection(Database.MONGO_VISITOR_COLLECTION_NAME);
		
		String randomId = generateRandomNum(20);
		DBObject visitor = new BasicDBObject("userId", userId)
					  .append("visitorId", randomId);
		
		collection.insert(visitor);
		
		return new Visitor(userId, randomId);
	}
	
	public static Visitor getUserWithVisitorId(String visitorId) throws UnknownHostException{
		DBCollection collection = Database.getCollection(Database.MONGO_VISITOR_COLLECTION_NAME);
		
		DBObject visitor = collection.findOne(new BasicDBObject("visitorId", visitorId));
		
		return visitor == null ? null : new Visitor((String)visitor.get("userId"), visitorId);
	}
	
	private static long generateRandomLong(){
		
		return ((long)random.nextInt(Integer.MAX_VALUE) << 32) + random.nextInt();
		
	}
	
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	/**
	 * method to return a random number in hex format 
	 * @param length the length of the random number note this is half the length of the returned string.
	 * @return a random hex string
	 */
	private static String generateRandomNum(int length) {
		
		byte bytes[] = new byte[length];
		random.nextBytes(bytes);
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
	public Visitor(){
		
	}
	
	public Visitor(String userId, String visitorId){
		this.userId = userId;
		this.visitorId = visitorId;
	}
	
	
}
