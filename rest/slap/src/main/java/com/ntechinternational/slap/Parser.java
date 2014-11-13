package com.ntechinternational.slap;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;

public class Parser {
	
	final String PAIR_SEPARATOR = "#", NAME_VALUE_SEPARATOR = ":";
	
	
	public Map<String, String> getNameValuePairs(String nameValuePairString){
		return getNameValuePairs(nameValuePairString, PAIR_SEPARATOR, NAME_VALUE_SEPARATOR);
	}

	public Map<String, String> getNameValuePairs(String nameValuePairString,
			String pairSeparator, String nameValueSeparator) throws StringIndexOutOfBoundsException, InvalidParameterException {
		
		if(nameValuePairString == null || pairSeparator == null || nameValueSeparator == null){
			throw new InvalidParameterException("Null parameter was passed.");
		}
		
		Map<String, String> parsedValues = new HashMap<String, String>();
		
		//step 1: separate name-value pair into distinct string using pair separator
		String[] nameValuePair = nameValuePairString.split(pairSeparator);
		for(int index = 0; index < nameValuePair.length; index++){
			//step 2: split each name-value pair using name value separator
			String[] nameValue = nameValuePair[index].split(nameValueSeparator);
			if(nameValue.length != 2)
				throw new StringIndexOutOfBoundsException("Passed name value pair is invalid ");
			
			parsedValues.put(nameValue[0], nameValue[1]); //Key is the first element and Value is the second element
		}
		
		return parsedValues;
	}

}
