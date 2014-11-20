package com.ntechinternational.slap;

import java.text.BreakIterator;
import java.text.CharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class provides methods to substitute a variables in a template.
 *
 */
public class Template{

	static final Pattern variablePattern = Pattern.compile("(\\[[^\\]]*\\])|(&\\w+)");
	static final Pattern simpleVariablePattern = Pattern.compile("(&\\w+)");

	public List<String> variablesWithValue;
	public List<String> variablesWithoutValue;
	public List<String> variablesWithDefaultValueOnly;
	
	
	final String template;
	public StringBuilder output;
	
	private Stack<Match> matches;
	
	public Template(String template){
		this.template = template;
		output = new StringBuilder(template);
		matches = new Stack<Match>();
		variablesWithValue = new ArrayList<String>();
		variablesWithoutValue = new ArrayList<String>();
		variablesWithDefaultValueOnly = new ArrayList<String>();
	}
	
	/**
	 * returns the string output of the text with variable values removed as necessary
	 * @param variableValues the map containing the value for the variable substitution
	 * @return the string with variable replaced
	 */
	public String process(Map<String, String> variableValues, Map<String, String> defaultValues){
		//match and record all optional and simple variables
		Matcher matcher = variablePattern.matcher(template);
		
		while(matcher.find()) matches.push(new Match(matcher.start(), matcher.end()));
		
		getOutput(variableValues, defaultValues);
		
		postProcessTemplate(output);
		
		
		return output.toString();
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
		retStr=retStr.replaceAll("(\\s){2,}"," ");
		//remove dot
		retStr= removeMultipleDot(retStr);
		
		retStr = retStr.replaceAll("\\)\\s+\\.", ").");
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


	/**
	 * returns the string output with variables replaced
	 */
	private String getOutput(Map<String, String> variableValues, Map<String, String> defaultValues){
		
		while(matches.size() != 0){
		Match match = matches.pop();
		
			if(output.charAt(match.startIndex) == '['){
				//this is an optional param, find individual variables
				//in it. If no variable can be substituted then remove the param.
				
				String optParam = output.substring(match.startIndex, match.endIndex);
				Matcher matcher = simpleVariablePattern.matcher(optParam);
				Stack<Match> subMatches = new Stack<Match>();
				
				//find all variables in the optional param enclosed in the braces 
				while(matcher.find()) subMatches.push(new Match(matcher.start(), matcher.end()));
				
				//now replace all the variables with values in the param 
				boolean replaced = false;
				StringBuilder subOutput = new StringBuilder(optParam);
				while(subMatches.size() != 0){
					Match subMatch = subMatches.pop();
					replaced |= replaceVariable(subOutput, subMatch, variableValues, defaultValues);
				}
				
				String toReplaceWith = ""; //replaces with empty string if no variable value substitution has been done.
				if(replaced){
					//only replace if variable has value
					//replace the variable in main output with string without braces
					toReplaceWith = subOutput.substring(1, subOutput.length()-1);
				}
				
				output.replace(match.startIndex, match.endIndex, toReplaceWith);
				
				LogUtil.trace("Found opt param " + optParam);
			}
			else{
				//replace the variables in string with value for simple variable
				replaceVariable(output, match, variableValues, defaultValues);
			}
		
		}
		
		return output.toString();
	}
	
	/**
	 * finds the matching variables in the string and replaces them with value in variable
	 * @param output the variable containing the actual string where the replacement is to be done.
	 * @param match the location where the match occurred and is to be replaced
	 * @param variableValues the list of values, that are to be used to replace the value
	 * @return <code>true</code> if a replacement has been made
	 * 		   <code>false</code> if no replacement has been done.
	 */
	private boolean replaceVariable(StringBuilder output, Match match, Map<String, String> variableValues, Map<String, String>defaultValues
			){
		boolean replaced = false;
		
		//perform simple replacement
		String variableName = output.substring(match.startIndex, match.endIndex);
		if(variableName.startsWith("&")){
			//all variables start with ampersand
			String value = variableValues.get(variableName); //find the variable's value in responded list
			
			if(value == null){ //if it is not found use the default list to populate
				value = defaultValues.get(variableName);
				if(value != null){
					variablesWithDefaultValueOnly.add(variableName);
				}
			}
			
			if(value != null){
				LogUtil.trace("Replacement done for " + variableName); 
				output.replace(match.startIndex, match.endIndex, value);
				replaced = true;
				variablesWithValue.add(variableName);
			}
			else{
				variablesWithoutValue.add(variableName);
			}
		}
		
		return replaced;
	}
	
	/**
	 * represents a simple data class, that will be used to store the Matched location.
	 *
	 */
	public class Match{
		public int startIndex;
		public int endIndex;
		
		public Match(int startIndex, int endIndex){
			this.startIndex = startIndex;
			this.endIndex = endIndex;
		}
	}
	
	
	/************* Static Methods/Helpers **************/
	
	/**
	 * this function extracts a list of key value pairs from a string
	 * The string must contain Key#1:Value#1,Key#2:Value#2,...,Key#N:Value#N
	 * @param variableList the string containing the list of key-value pairs
	 * @return returns a Map with the key value pairs
	 */
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


