package com.ntechinternational.slap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;



public class ConfigurationMap {
	public String requestURL;
	public String backendDocNode;
	public List<RequestParam> requestParams;
	/**
	 * maps the backend response's variable name to server's/incoming request's variable name
	 */
	public Map<String, String> responseMappings;
	
	/**
	 * maps the incoming request variable name to backend response name. Inverse of response mappings
	 */
	public Map<String, String> requestMappings;
	
	
	
	
	public static class RequestParam{
		public String paramName;
		public String paramValue;
		public boolean isRequired;
		public String paramType;
		
		public RequestParam(){
			
		}
		public RequestParam(String paramName,String paramValue, boolean isRequired, String paramType){
			this.paramName = paramName;
			this.paramValue = paramValue;
			this.paramType = paramType;
			this.isRequired = isRequired;
		}
	}
	
	private ConfigurationMap(){
		this.requestParams = new ArrayList<RequestParam>();
		this.responseMappings = new HashMap<String, String>();
		this.requestMappings = new HashMap<String, String>();
	}
	
	//TODO: to change to singleton pattern
	private static ConfigurationMap singletonMap = null;
	
	public static synchronized ConfigurationMap getConfig(String mapFileName) throws ConfigurationException{
		if(singletonMap == null){
			singletonMap = new ConfigurationMap();
			
			try{ 
				XMLConfiguration config = new XMLConfiguration(mapFileName); //load the configuration file
				
				singletonMap.requestURL = config.getString("request.url");
				
				//get all the param required for request
				readRequestParam(config.configurationsAt("request.requiredParam.param"), true);
				readRequestParam(config.configurationsAt("request.otherParam.param"), false); //false because param is not required
			
				singletonMap.backendDocNode = config.getString("response.documentNode");
				for(HierarchicalConfiguration mapConfig : config.configurationsAt("response.mappings.fieldName")){
					String  sourceName = mapConfig.getString("[@source]"),
							targetName = mapConfig.getString("[@target]");
					
					singletonMap.responseMappings.put(sourceName, targetName); //maps backend name to incoming request name
					singletonMap.requestMappings.put(targetName, sourceName); //maps incoming request param name to backend name
				}
			}
			catch(ConfigurationException ex){
				throw ex;
			}
		}
		return singletonMap;
	}

	private static void readRequestParam(List<HierarchicalConfiguration> paramConfigs, boolean isRequired) {
		for(HierarchicalConfiguration conf : paramConfigs){
			String  paramName = conf.getString("[@name]"),
					paramValue = conf.getString("[@value]"), //get the value from the passed list
					paramType = conf.getString("[@type]");
			
			System.out.println("Found param " + paramName);
			
			RequestParam param = new ConfigurationMap.RequestParam(paramName, paramValue, isRequired, paramType);
			
			singletonMap.requestParams.add(param);
		}
	}
	
}
