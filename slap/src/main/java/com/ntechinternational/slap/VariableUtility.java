package com.ntechinternational.slap;

import java.util.HashMap;
import java.util.Map;

/**
 * represents the class responsible for utilities that fetch the list of variables
 */
public class VariableUtility {
	
	public static Map<String, String>getVariablesFromString(String variableList){
		
		Map<String, String> variableMap = new HashMap<String, String>();
		
		String[] values = variableList.split(","); //list has key value pairs separated by comma (,)
		for(int index = 0; index < values.length; index++){
			String[] kvs = values[index].split(":"); //each key value pair is separated by colon(:)
			
			if(kvs.length == 2){
				variableMap.put(kvs[0].trim(), kvs[1].trim());
			}
		}
		
		return variableMap;
		
	}
	
	
}
