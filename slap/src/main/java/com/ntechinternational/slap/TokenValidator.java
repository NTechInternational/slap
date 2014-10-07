package com.ntechinternational.slap;

import javax.ws.rs.core.MultivaluedMap;

public class TokenValidator {
	
		/**
	 * This method checks if a valid token Id exists for a visitorId, if it does returns the token, else
	 * creates one and returns the token
	 * @param visitorId
	 * @return Token mapped to the visitor Id
		 * @throws Exception 
	 */
	public Token checkTokenId(long visitorId, MultivaluedMap<String, String> params) throws Exception{
		Token token = new Token();
		
		System.out.println("Validating visitor Id");
		Visitor visitor = Visitor.getUserWithVisitorId(visitorId);
		
		if(visitor == null){
			throw new Exception("Unknown visitor Id");
		}
		//return the token Id
		token.tokenId = visitor.userId;
		
		return token;
	}
}
