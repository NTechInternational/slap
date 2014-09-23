package com.ntechinternational.slap;

public class TokenValidator {

	/**
	 * This method checks if a valid token Id exists for a visitorId, if it does returns the token, else
	 * creates one and returns the token
	 * @param visitorId
	 * @return Token mapped to the visitor Id
	 */
	public Token checkTokenId(long visitorId){
		Token token = new Token();
		
		//TODO: add method that validates the visitor id and maps it to corresponding token and other map values
		//for now the token is sampletokenid
		token.tokenId = "sampletokenid";
		
		return token;
	}
}
