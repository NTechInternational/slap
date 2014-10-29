package com.ntechinternational.slap;

import java.net.UnknownHostException;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class Substitutor {
	/**
	 * Substitutes all the variables in the challenge and returns the new challenge
	 * @return
	 * @throws UnknownHostException 
	 */
	public List<Map<String, Object>> substituteVariables(List<Map<String, Object>> items, String visitorId) throws UnknownHostException{
		BasicDBObject query = new BasicDBObject("visitorId", visitorId);
		DBCursor cursor = Database.getCollection(Database.MONGO_QUESTION_COLLECTION_NAME).find(query);
		boolean wasReplaced = false;
		
		List<StringBuilder> allItemTemplates = new ArrayList<StringBuilder>();
		for(Map<String, Object> item : items){
			String itemTemplate = (String) item.get("itemtemplate");
			
			allItemTemplates.add(new StringBuilder(itemTemplate));
		}
		
		
		
		
		//find all the responded variables and use them to replace all text in items
		while(cursor.hasNext()){
			wasReplaced = true;
			DBObject storedResponse = cursor.next(); //get the response for a question
			BasicDBList variables = (BasicDBList)storedResponse.get("variables");
			
			for(int index = 0; index < variables.size(); index++){
				DBObject variable = (DBObject)variables.get(index);
				String key = variable.keySet().iterator().next();
				String value = (String)variable.get(key);
				
				key = "&" + key;
				
				for(StringBuilder itemTemplate : allItemTemplates){
					
					replaceAll(itemTemplate, key, value);
					
					postProcessTemplate(itemTemplate);
					
					System.out.println(itemTemplate);
					
				}
				
			}
		}
		
		//rewrite the string in the param object
		if(wasReplaced){
			int index = 0;
			for(Map<String, Object> item : items){
				
				item.put("itemtemplate", allItemTemplates.get(index).toString());
				
				index++;
			}
		}
		
		return items;
	}
	
	private void replaceAll(StringBuilder builder, String from, String to)
	{
	    int index = builder.indexOf(from);
	    while (index != -1)
	    {
	        builder.replace(index, index + from.length(), to);
	        index += to.length(); // Move to the end of the replacement
	        index = builder.indexOf(from, index);
	    }
	}

	
	private void postProcessTemplate(StringBuilder itemTemplate){
		// sentence parsing and convert first letter to uppercase
		BreakIterator boundary = BreakIterator.getSentenceInstance(Locale.US);
	    String source =itemTemplate.toString();
	    itemTemplate.setLength(0);
		itemTemplate.append(parseConvertSentence(boundary, source));
	}
	
private StringBuilder parseConvertSentence(BreakIterator bi, String source) {
		
		StringBuilder sb = new StringBuilder();
		bi.setText(source);

		int lastIndex = bi.first();
		while (lastIndex != BreakIterator.DONE) {
			int firstIndex = lastIndex;
			lastIndex = bi.next();

			if (lastIndex != BreakIterator.DONE) {
				String sentence = source.substring(firstIndex, lastIndex);
				
				sb.append(applySentenceRules(sentence));
				sb.append(" ");
				
			}
		}
		int len = sb.length();
		if (sb.lastIndexOf(" ")==len-1)
			sb.setLength(len-1);
		
		return sb;
	}

	private String applySentenceRules(String sentence)
	{
		String retStr = removeChar(convertUpperCase(sentence));
		//remove multiple whitespace
		retStr=retStr.replaceAll("(\\s)\\1","");
		//remove dot
		retStr= removeMultipleDot(retStr);
		return retStr;
	}
	
	private String convertUpperCase(String str)
	{
		StringBuilder sb = new StringBuilder(str); 

		Pattern pattern = Pattern.compile("(^|\\.|!|\\?)\\s*(\\w)");
		Matcher matcher = pattern.matcher(sb);
		while (matcher.find())
			sb.replace(matcher.end() - 1, matcher.end(), matcher.group(2).toUpperCase());
		return sb.toString();
	}
	
	private String removeChar(String str)
	{
		StringBuilder sb = new StringBuilder(str); 

		Pattern pattern = Pattern.compile("(\\[|\\])");
		Matcher matcher = pattern.matcher(sb);
		while (matcher.find())
			sb.replace(matcher.end() - 1, matcher.end(), "#");
		
		return sb.toString().trim().replaceAll("#","");
	}
	
	private String removeMultipleDot(String str)
	{
		StringBuilder buffy = new StringBuilder(str);
		
		Pattern pattern = Pattern.compile("(\\.\\s\\.{1,2})");
		Matcher matcher = pattern.matcher(buffy);
		
		while (matcher.find())
			buffy.replace(matcher.end() - 1, matcher.end(), "#");
		
		return buffy.toString().trim().replaceAll("#","");
	}
	
}
