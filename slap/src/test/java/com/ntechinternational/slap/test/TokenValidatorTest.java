package com.ntechinternational.slap.test;

import static org.junit.Assert.*;
import java.security.SecureRandom;
import org.junit.Test;
import com.ntechinternational.slap.Token;
import com.ntechinternational.slap.TokenValidator;

/**
 * Test class that validates that the token validator works according to design.
 */
public class TokenValidatorTest {
	
	private SecureRandom randomNumberGenerator;
	
	public TokenValidatorTest(){
		randomNumberGenerator = new SecureRandom();
	}
	
	
	@Test
	public void givenVisitorIdExists_ValidTokenIdIsReturned(){
		//check whether the same token id setup is returned or not
		final long visitorId = randomNumberGenerator.nextLong();
		
		//setup token id validation in this state no token should exist so
		//new token should be returned
		Token token = new TokenValidator().checkTokenId(visitorId);
		
		//check it again
		Token newToken = new TokenValidator().checkTokenId(visitorId);
		
		assertEquals(token.tokenId, newToken.tokenId);
	}
	
	@Test
	public void givenVisitorIdDoesntExist_ValidTokenIdIsReturned(){
		final long visitorId = randomNumberGenerator.nextLong();
		
		Token token = new TokenValidator().checkTokenId(visitorId);
		
		assertTrue("Expected non empty token id but found it empty" ,
				token.tokenId != null && !token.tokenId.isEmpty()); //non-empty tokenId is returned
	}
	

}
