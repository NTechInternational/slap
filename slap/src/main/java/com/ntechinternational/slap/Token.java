package com.ntechinternational.slap;

import java.util.HashMap;
import java.util.Map;

public class Token {

	public String tokenId;
	private Map<String, String> nameValuePairs;
	
	public Map<String, String> getNameValuePairs(){
		return nameValuePairs;
	}
	
	
	Token(){
		nameValuePairs = new HashMap<String, String>();
	}
	
}
