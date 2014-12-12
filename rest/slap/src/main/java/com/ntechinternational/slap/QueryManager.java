package com.ntechinternational.slap;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

public class QueryManager {
	
	private static final String FILTER_QUERY_SEPARATOR = ":";
	private static final String FILTER_QUERY_NAME = "fq";
	private static final String ROW_SIZE_PARAM_NAME = "rows";
	public static final String TYPE_FILTER = "filter";
	public static final String TYPE_DEFAULT = "default";
	private static final String DEFAULT_MEDIATYPE = MediaType.APPLICATION_XML;
	
	private MultivaluedMap<String, String> params = new MultivaluedHashMap<String, String>();
	private int returnSetSize = -1;
	private String queriedURL = null;
	private ConfigurationMap config = null;
	private String requestPath;
	private List<String> filtersToAdd = new ArrayList<String>();
	private String mediaType;
	
	public static List<String> queryLog;
	
	/**
	 * queries a given resource and returns the response as string. The query string is constructed on the
	 * basis of the configuration passed and query parameters passed
	 * @param visitorId the id of the visitor
	 * @param queryParams the map of values of query parameters
	 * @param config the configuration that defines the mapping of server response name, to request construction rules
	 * @param resourcePath the location of the resource
	 * @return a string response retrieved from the server
	 * @throws Exception
	 */
	public String query(String visitorId,
			MultivaluedMap<String, String> queryParams,
			ConfigurationMap config,
			String resourcePath,
			MultivaluedMap<String, String> paramsToAdd) throws Exception {
		
		Client client = ClientBuilder.newClient();
		WebTarget target = client.target(config.requestURL).path(resourcePath);
		
		//prepare request with correct params as defined in the mapping file
		for(int index = 0; index < config.requestParams.size(); index++){
			ConfigurationMap.RequestParam requestParam = config.requestParams.get(index);
			
			LogUtil.trace("In the loop " + requestParam.paramName + " with default value " + requestParam.paramValue);
			
			//Lets map the JavaScript parameter to name to backend server param name as defined in the mapping
			//if necessary
			String clientParamName = requestParam.paramName;
			if(config.responseMappings.containsKey(clientParamName)){
				//if a mapping is found then replace the param name
				clientParamName = config.responseMappings.get(clientParamName);
			}
			
			List<String> valueToPass = null;
			
			//if the query parameter doesn't contain a required parameter, an exception is generated
			if(queryParams.containsKey(clientParamName) == false && requestParam.isRequired){
				//System.err.println("Exception thrown required param not found");
				//throw new Exception("Missing required parameter: " + clientParamName);
				//TODO: we might need to package the exception better and give a JSON exception
				//valueToPass = new ArrayList<String>();
				//valueToPass.add(requestParam.paramValue);	
			}
			
			if(queryParams.containsKey(clientParamName)){
				//this is the request param name that has to be sent
				valueToPass = queryParams.get(clientParamName);
			}
			else if(requestParam.paramType != null && requestParam.paramType.startsWith(TYPE_DEFAULT) && requestParam.paramValue != null){
				//in case the client param is missing and the current param is of type default use the default value
				valueToPass = new ArrayList<String>();
				valueToPass.add(requestParam.paramValue);
			}
			
			//if we have some to send
			if(valueToPass != null){
				if(requestParam.paramType != null && requestParam.paramType.endsWith(TYPE_FILTER)){
					//if it is filter than pass as fq param:value
					for(String val : valueToPass){
						target = target.queryParam(FILTER_QUERY_NAME, requestParam.paramName + FILTER_QUERY_SEPARATOR + val);
					}
				}
				else{
					for(String val : valueToPass) {
						target = target.queryParam(requestParam.paramName, val);
					}
				}
			}
		}
		
		if(paramsToAdd != null){
			for(String paramKey : paramsToAdd.keySet()){
				List<String> values = paramsToAdd.get(paramKey);
				for(String value : values){
					target = target.queryParam(paramKey, value);
				}
			}
			
		}
		//includes all the results
		target = target.queryParam("q", "*:*");
		LogUtil.debug("Querying " + target.getUri());
		if(queryLog != null)
			queryLog.add(target.getUri().toString());
		
		Response response = target.request(DEFAULT_MEDIATYPE).get(); //set the accepted media type to one defined
		
		return response.readEntity(String.class);

	}

	/**
	 * adds the passed input params to be sent back to the server
	 */
	public QueryManager addInputParams(MultivaluedMap<String, String> params){
		params.putAll(params);
		return this;
	}
	

	/**
	 * sets the size of the row that must be returned
	 */
	public QueryManager setReturnSetSize(int size){
		this.returnSetSize = size;
		
		return this;
	}
	
	/**
	 * sets the configuration map that contains the details of various configuration such as variable name mappings.
	 */
	public QueryManager setConfig(ConfigurationMap config){
		this.config  = config;
		
		return this;
	}
	
	/**
	 * sets the path where the query is to be fired
	 */
	public QueryManager setRequestPath(String path){
		this.requestPath = path;
		
		return this;
	}
	
	/**
	 * sets the media type of the query to be fired to the backend
	 */
	public QueryManager setMediaType(String mediaType){
		this.mediaType = mediaType;
		
		return this;
	}
	
	/**
	 * adds a filter param that must be sent to the server
	 * @param filter the name of the filter param to send
	 * @param value the value of the filter param
	 * @return
	 */
	public QueryManager addFilterParam(String filter, String value ){
		String mappedFilterName = this.config.requestMappings.get(filter.toLowerCase());
		if(mappedFilterName == null)
			mappedFilterName = filter; //if there is no mapped name of the filter use the name sent from the client
		
		this.filtersToAdd.add(mappedFilterName + FILTER_QUERY_SEPARATOR + value);
		return this;
	}
	
	/**
	 * returns the string URL of the last queried path
	 */
	public String getQueriedURL(){
		return this.queriedURL ;
	}
	
	/**
	 * resets the query manager's internal state clearing all but config
	 * @return
	 */
	public QueryManager reset(){
		this.params.clear();
		this.filtersToAdd.clear();
		this.returnSetSize = -1;
		this.queriedURL = null;
		this.requestPath = null;
		this.mediaType = DEFAULT_MEDIATYPE;
		
		return this;
	}
	
	public QueryManager(){
		this.reset();
	}
	
	
	/**
	 * executes the query and returns the returned value as string
	 */
	public String query(){
		return query(true); //always fire query to backend server
	}
	
	/**
	 * queries a given backend server
	 * @param performActualQuery if set to true fires a query to backend server else doesn't fire (Useful for tests)
	 */
	public String query(boolean performActualQuery){
		Client client = ClientBuilder.newClient();
		WebTarget target = client.target("http://localhost:1234").path(requestPath);
		
		target = addMappedVariables(target);
		
		//add additional filter params
		for(int index = 0, length = this.filtersToAdd.size(); index < length; index++){
			target = target.queryParam(FILTER_QUERY_NAME, this.filtersToAdd.get(index));
		}
		
		this.queriedURL = target.getUri().toString();
		LogUtil.debug("Querying: " + this.queriedURL);
		
		return performActualQuery ? executeQuery(target) : "";
	}
	
	/**
	 * fires a query to backend server and returns string containing the response
	 */
	private String executeQuery(WebTarget target){
		Response response = target.request(mediaType).get(); //set the accepted media type to one defined
		
		return response.readEntity(String.class);
	}

	/**
	 * method that takes the configured mapped values and checks if they are present in query param and 
	 * sets them accordingly
	 */
	private WebTarget addMappedVariables(WebTarget target) {
		
		//prepare request with correct params as defined in the mapping file
		for(int index = 0; index < config.requestParams.size(); index++){
			ConfigurationMap.RequestParam requestParam = config.requestParams.get(index);
			
			LogUtil.trace("In the loop " + requestParam.paramName + " with default value " + requestParam.paramValue);
			
			//Lets map the JavaScript parameter to name to backend server param name as defined in the mapping
			//if necessary
			String clientParamName = requestParam.paramName;
			

			
			if(config.responseMappings.containsKey(clientParamName)){
				//if a mapping is found then replace the param name
				clientParamName = config.responseMappings.get(clientParamName);
			}
			
			List<String> valueToPass = null;
			
			//if the query parameter doesn't contain a required parameter, an exception is generated
			if(params.containsKey(clientParamName) == false && requestParam.isRequired){
				//System.err.println("Exception thrown required param not found");
				//throw new Exception("Missing required parameter: " + clientParamName);
				//TODO: we might need to package the exception better and give a JSON exception
				//valueToPass = new ArrayList<String>();
				//valueToPass.add(requestParam.paramValue);	
			}
			
			if(params.containsKey(clientParamName)){
				//this is the request param name that has to be sent
				valueToPass = params.get(clientParamName);
			}
			else if(requestParam.paramType != null && requestParam.paramType.equals(TYPE_DEFAULT) && requestParam.paramValue != null){
				//in case the client param is missing and the current param is of type default use the default value
				valueToPass = new ArrayList<String>();
				if(clientParamName.equals(ROW_SIZE_PARAM_NAME) && this.returnSetSize != -1){
					valueToPass.add(String.valueOf(this.returnSetSize)); //if return set size has been configured uses it
				}
				else{
					valueToPass.add(requestParam.paramValue);
				}
			}
			
			//if we have some to send
			if(valueToPass != null){
				if(requestParam.paramType != null && requestParam.paramType.equals(TYPE_FILTER)){
					//if it is filter than pass as fq param:value
					for(String val : valueToPass){
						target = target.queryParam(FILTER_QUERY_NAME, requestParam.paramName + FILTER_QUERY_SEPARATOR + val);
					}
				}
				else{
					for(String val : valueToPass) {
						target = target.queryParam(requestParam.paramName, val);
					}
				}
			}
		}
		
		return target;
		
	}
	


}
