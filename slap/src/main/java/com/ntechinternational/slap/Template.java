package com.ntechinternational.slap;

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

	
	final String template;
	public StringBuilder output;
	
	private Stack<Match> matches;
	
	public Template(String template){
		this.template = template;
		output = new StringBuilder(template);
		matches = new Stack<Match>();
	}
	
	/**
	 * returns the string output of the text with variable values removed as necessary
	 * @param variableValues the map containing the value for the variable substitution
	 * @return the string with variable replaced
	 */
	public String process(Map<String, String> variableValues){
		//match and record all optional and simple variables
		Matcher matcher = variablePattern.matcher(template);
		
		while(matcher.find()) matches.push(new Match(matcher.start(), matcher.end()));
		
		return getOutput(variableValues);
	}
	
	/**
	 * returns the string output with variables replaced
	 */
	private String getOutput(Map<String, String> variableValues){
		
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
					replaced |= replaceVariable(subOutput, subMatch, variableValues);
				}
				
				String toReplaceWith = ""; //replaces with empty string if no variable value substitution has been done.
				if(replaced){
					//only replace if variable has value
					//replace the variable in main output with string without braces
					toReplaceWith = subOutput.substring(1, subOutput.length()-1);
				}
				
				output.replace(match.startIndex, match.endIndex, toReplaceWith);
				
				System.out.println("Found opt param " + optParam);
			}
			else{
				//replace the variables in string with value for simple variable
				replaceVariable(output, match, variableValues);
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
	private boolean replaceVariable(StringBuilder output, Match match, Map<String, String> variableValues){
		boolean replaced = false;
		
		//perform simple replacement
		String variableName = output.substring(match.startIndex, match.endIndex);
		if(variableName.startsWith("&")){
			//all variables start with ampersand
			String value = variableValues.get(variableName);
			if(value != null){
				System.out.println("Replacement done for " + variableName); 
				output.replace(match.startIndex, match.endIndex, value);
				replaced = true;
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
	
}


