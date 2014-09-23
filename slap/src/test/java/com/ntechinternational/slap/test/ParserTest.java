package com.ntechinternational.slap.test;

import static org.junit.Assert.*;

import java.util.Map;
import org.junit.Test;
import com.ntechinternational.slap.Parser;

public class ParserTest {
	
	final String PAIR_SEPARATOR = "#", NAME_VALUE_SEPARATOR = ":";

	@Test
	public void givenNameValueString_ItIsParsedCorrectly(){
		//This test ensures that given the string of type
		//sell:p101#client:c301
		
		String nameValuePairString = "sell:p101#client:c301";
		
		Parser parser = new Parser();
		Map<String, String> nameValuePairs = parser.getNameValuePairs(nameValuePairString, PAIR_SEPARATOR, NAME_VALUE_SEPARATOR);
		
		assertEquals(nameValuePairs.size(), 2);
		assertEquals(nameValuePairs.get("sell"), "p101");
		assertEquals(nameValuePairs.get("client"), "c301");
	}
}
