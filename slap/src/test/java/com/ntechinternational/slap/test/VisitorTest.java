package com.ntechinternational.slap.test;

import static org.junit.Assert.*;

import java.net.UnknownHostException;

import org.junit.Test;

import com.ntechinternational.slap.Visitor;


public class VisitorTest {
	
	private static final String VISITOR_USER_ID = "123123123";
	
	@Before
	public void setup(){
		try{
			Visitor.removeVisitorIfExists(VISITOR_USER_ID);	
		}
		catch(Exception ex){
			//do nothing
		}
	}

	@Test
	public void givenUnknownUserId_VisitorIdIsReturned(){
		//TODO: use test monogodb instead of real for test cases 
		try {
			Visitor visitor = Visitor.getVisitorFor(VISITOR_USER_ID);
			assertNull(visitor);
			
			visitor = Visitor.createVisitorFor(VISITOR_USER_ID);
			assertNotNull(visitor.visitorId);
			assertSame(visitor.userId, VISITOR_USER_ID);
			
			
			String visitorId = visitor.visitorId;
			visitor = Visitor.getVisitorFor(VISITOR_USER_ID);
			assertEquals(visitorId, visitor.visitorId);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
}
