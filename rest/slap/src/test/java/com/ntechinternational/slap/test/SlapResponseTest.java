package com.ntechinternational.slap.test;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.Before;
import org.junit.Test;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.ntechinternational.slap.Database;
import com.ntechinternational.slap.QueryManager;
import com.ntechinternational.slap.SlapResponse;
import com.ntechinternational.slap.SlapRestImpl;
import com.ntechinternational.slap.Visitor;

public class SlapResponseTest {
	List<String> uriLog; 
	
	@Before
	public void setup(){
		uriLog = new ArrayList<String>();
		QueryManager.queryLog = uriLog;
	}

	@Test
	public void GivenInvalidVisitorId_ErrorMessageIsReturned () throws Exception{
		SlapRestImpl impl = new SlapRestImpl();
		impl.setVisitorId("Invalid Visitor Id");
		MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<String, String>();
		
		SlapResponse response = impl.processRequest(queryParams);
		
		assertTrue(response.errorDescription.length() > 0);
	}
	
	@Test
	public void GivenInitialInteraction_1QuestionAnd5ItemIsReturned() throws Exception{
		SlapRestImpl impl = new SlapRestImpl();
		Visitor v = Visitor.createVisitorFor("RandomUser10000");
		
		impl.setVisitorId(v.visitorId);
		MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<String, String>();
		
		SlapResponse response = impl.processRequest(queryParams);
		
		assertEquals(5, response.items.size());
		assertEquals(1, response.questions.size());
		assertTrue(response.errorDescription == null);
	}
	
	@Test
	public void GivenVariousInteraction_CorrectUrlIsCalled() throws Exception{
		SlapRestImpl impl = new SlapRestImpl();
		Visitor v = Visitor.createVisitorFor("RandomUser10100");
		
		impl.setVisitorId(v.visitorId);
		MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<String, String>();
		
		SlapResponse response = impl.processRequest(queryParams);
		
		//select the first question's model
		//i.e. BusinessModel:Membership
		queryParams.putSingle("facet", "BusinessModel:Membership");
		queryParams.putSingle("qid", "3001");
		queryParams.putSingle("type", "submit");
		uriLog.clear();
		response = impl.processRequest(queryParams);
		
		boolean uriContainsBusinessModel = false;
		for(String uri : uriLog){
			if(uri.contains("Membership")){
				uriContainsBusinessModel = true; //both challenge and question should contain facet
			}
			else{
				uriContainsBusinessModel = false;
				break;
			}
		}
		
		assertTrue(uriContainsBusinessModel);
		
		
		//select the second questions's model
		//i.e. Motivation:BenefitsCharity
		queryParams.remove("facet");
		queryParams.remove("qid");
		queryParams.putSingle("qid", "3002");
		queryParams.putSingle("facet", "Motivation:BenefitsCharity");
		uriLog.clear();
		response = impl.processRequest(queryParams);
		boolean uriContainsMotivationAndBusinessModel = false;
		
		for(String uri : uriLog){
			if(uri.contains("Membership")){
				uriContainsMotivationAndBusinessModel = uri.contains("BenefitsCharity"); //both challenge and question shoutld contain facet
			}
			else{
				uriContainsMotivationAndBusinessModel = false;
				break;
			}
		}
		
		assertTrue(uriContainsMotivationAndBusinessModel);
		
		
		//select the item 1012
		queryParams.remove("facet");
		queryParams.remove("qid");
		queryParams.remove("type");
		queryParams.putSingle("itemId", "1012");
		queryParams.putSingle("type", "select");
		uriLog.clear();
		response = impl.processRequest(queryParams);
		boolean uriContainsVariables = false;
		for(String uri : uriLog){
			if(uri.contains("%26Reward+OR+%26Offering+OR+%26OfferingAmount+OR+%26OfferAction")){
				uriContainsVariables = true;
				break;
			}
		}
		
		assertTrue(uriContainsVariables);
		
		
		queryParams.clear();
		queryParams.putSingle("type", "startOver");
		uriLog.clear();
		response = impl.processRequest(queryParams);
		uriContainsBusinessModel = false;
		for(String uri: uriLog){
			if(uri.contains("BusinessModel_ss:All")){
				uriContainsBusinessModel = true;
				break;
			}
		}
		
		assertTrue(uriContainsBusinessModel);
		
		
	}
	
	
	@Test
	public void GivenStartOverInteraction_SessionIsSaved() throws Exception{
		//setup
		SlapRestImpl impl = new SlapRestImpl();
		Visitor v = Visitor.createVisitorFor("RandomUser10200");
		
		impl.setVisitorId(v.visitorId);
		MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<String, String>();
		
		SlapResponse response = impl.processRequest(queryParams);
		
		//select the first question's model
		//i.e. BusinessModel:Membership
		queryParams.putSingle("facet", "BusinessModel:SomethingCool");
		queryParams.putSingle("qid", "3001");
		queryParams.putSingle("type", "submit");
		uriLog.clear();
		response = impl.processRequest(queryParams);
		
		//actual action
		queryParams.clear();
		queryParams.putSingle("type", "startOver");
		
		impl.processRequest(queryParams);
		
		DBCollection collection = Database.getCollection(Database.MONGO_VISITOR_SESSION_START_OVER_COLLECTION_NAME);
		DBCursor cursor = collection.find(new BasicDBObject("visitorId", v.visitorId).append("questions.facets.BusinessModel", "SomethingCool"));
		int count = 0;
		while(cursor.hasNext()){
			cursor.next();
			count++;
		}
		
		assertTrue(count > 0);
	}
	
	@Test
	public void GivenDoneInteraction_SessionIsSaved() throws Exception{
		SlapRestImpl impl = new SlapRestImpl();
		Visitor v = Visitor.createVisitorFor("RandomUser10200");
		
		impl.setVisitorId(v.visitorId);
		MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<String, String>();
		
		SlapResponse response = impl.processRequest(queryParams);
		
		//select the first question's model
		//i.e. BusinessModel:Membership
		queryParams.putSingle("facet", "BusinessModel:SomethingCool");
		queryParams.putSingle("qid", "3001");
		queryParams.putSingle("type", "submit");
		uriLog.clear();
		response = impl.processRequest(queryParams);
		
		//actual action
		queryParams.clear();
		queryParams.putSingle("type", "done");
		queryParams.putSingle("text", "All Done");
		
		impl.processRequest(queryParams);
		
		DBCollection collection = Database.getCollection(Database.MONGO_VISITOR_SESSION_COLLECTION_NAME);
		DBCursor cursor = collection.find(new BasicDBObject("visitorId", v.visitorId).append("finalText", "All Done"));
		int count = 0;
		while(cursor.hasNext()){
			cursor.next();
			count++;
		}
		
		assertTrue(count > 0);
	}
	
	
	@Test
	public void GivenALanaguageIsSelected_ItIsCorrectlyPassedToBackend() throws Exception{
		SlapRestImpl impl = new SlapRestImpl();
		Visitor v = Visitor.createVisitorFor("RandomUser10300");
		
		impl.setVisitorId(v.visitorId);
		MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<String, String>();
		
		SlapResponse response = impl.processRequest(queryParams);
		
		//select the first question's model
		queryParams.putSingle("language", "fr");
		uriLog.clear();
		response = impl.processRequest(queryParams);
		
		for(String uri: uriLog){
			assertTrue(uri.contains("fq=language_s:fr"));
		}
	}
	
	@Test
	public void GivenNoLanaguageIsSelected_DefaultLanguageIsPassed() throws Exception{
		SlapRestImpl impl = new SlapRestImpl();
		Visitor v = Visitor.createVisitorFor("RandomUser10300");
		
		impl.setVisitorId(v.visitorId);
		MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<String, String>();
		
		SlapResponse response = impl.processRequest(queryParams);
		
		//select the first question's model
		uriLog.clear();
		response = impl.processRequest(queryParams);
		
		for(String uri: uriLog){
			assertTrue(uri.contains("fq=language_s:en"));
		}
	}
	
	@Test
	public void GivenBackIsPressed_PreviousSubmissionIsUndone() throws Exception {
		//setup
		SlapRestImpl impl = new SlapRestImpl();
		Visitor v = Visitor.createVisitorFor("RandomUser10100");
		
		impl.setVisitorId(v.visitorId);
		MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<String, String>();
		
		SlapResponse response = impl.processRequest(queryParams);
		
		//select the first question's model
		//i.e. BusinessModel:Membership
		queryParams.putSingle("facet", "BusinessModel:Membership");
		queryParams.putSingle("qid", "3000");
		queryParams.putSingle("type", "submit");
		uriLog.clear();
		response = impl.processRequest(queryParams);
		

		queryParams.clear();
		//select the first question's model
		//i.e. BusinessModel:Membership
		queryParams.putSingle("facet", "Goal:StealShare");
		queryParams.putSingle("qid", "3001");
		queryParams.putSingle("type", "submit");
		uriLog.clear();
		response = impl.processRequest(queryParams);
		
		for(String uri : uriLog){
			assertTrue(uri.contains("StealShare"));
		}
		
		//perform back action
		queryParams.clear();
		queryParams.putSingle("type", "back");
		uriLog.clear();
		response = impl.processRequest(queryParams);
		
		for(String uri : uriLog){
			assertFalse(uri.contains("StealShare"));
			assertTrue(uri.contains("Membership"));
		}
		
		//perform another back action
		uriLog.clear();
		response = impl.processRequest(queryParams);
		
		for(String uri : uriLog){
			assertFalse(uri.contains("Membership"));
		}
		
		
		queryParams.clear();
		queryParams.putSingle("type", "select");
		queryParams.putSingle("itemId", "1012");
		uriLog.clear();
		response = impl.processRequest(queryParams);
		
		boolean containsItemId = false;
		for(String uri : uriLog){
			if(uri.contains("1012")){
				containsItemId = true;
				break;
			}
		}
		
		assertTrue(containsItemId);
		
		
		//perform back action
		queryParams.clear();
		queryParams.putSingle("type", "back");
		uriLog.clear();
		response = impl.processRequest(queryParams);
		
		for(String uri : uriLog){
			assertFalse(uri.contains("1012"));
		}
	}
	
	@Test
	public void GivenResetIsPressed_InteractionLogIsCleared() throws Exception {
		//setup
		SlapRestImpl impl = new SlapRestImpl();
		Visitor v = Visitor.createVisitorFor("RandomUser10103");
		
		impl.setVisitorId(v.visitorId);
		MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<String, String>();
		
		SlapResponse response = impl.processRequest(queryParams);
		
		//select the first question's model
		//i.e. BusinessModel:Membership
		queryParams.putSingle("facet", "BusinessModel:Membership");
		queryParams.putSingle("qid", "3000");
		queryParams.putSingle("type", "submit");
		uriLog.clear();
		response = impl.processRequest(queryParams);
		

		queryParams.clear();
		//select the first question's model
		//i.e. BusinessModel:Membership
		queryParams.putSingle("facet", "Goal:StealShare");
		queryParams.putSingle("qid", "3001");
		queryParams.putSingle("type", "submit");
		uriLog.clear();
		response = impl.processRequest(queryParams);
		
		for(String uri : uriLog){
			assertTrue(uri.contains("StealShare"));
		}
		
		//perform back action
		queryParams.clear();
		queryParams.putSingle("type", "startOver");
		uriLog.clear();
		response = impl.processRequest(queryParams);
		
		for(String uri : uriLog){
			assertFalse(uri.contains("StealShare"));
			assertFalse(uri.contains("Membership"));
		}
		
		assertTrue(Database.getCollection("interactionLog").findOne(new BasicDBObject("visitorId", v.visitorId)) == null);
		
		
	}
	

}
