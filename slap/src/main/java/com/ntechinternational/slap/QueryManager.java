package com.ntechinternational.slap;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

public class QueryManager {
	
	private static final String FILTER_QUERY_SEPARATOR = ":";
	private static final String FILTER_QUERY_NAME = "fq";
	public static final String TYPE_FILTER = "filter";
	public static final String TYPE_DEFAULT = "default";
	private static final String DEFAULT_MEDIATYPE = MediaType.APPLICATION_XML;
	
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
	public String query(long visitorId,
			MultivaluedMap<String, String> queryParams,
			ConfigurationMap config,
			String resourcePath) throws Exception {
		
		Client client = ClientBuilder.newClient();
		WebTarget target = client.target(config.requestURL).path(resourcePath);
		
		//prepare request with correct params as defined in the mapping file
		for(int index = 0; index < config.requestParams.size(); index++){
			ConfigurationMap.RequestParam requestParam = config.requestParams.get(index);
			
			System.out.println("In the loop " + requestParam.paramName + " with default value " + requestParam.paramValue);
			
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
				System.err.println("Exception thrown required param not found");
				throw new Exception("Missing required parameter: " + clientParamName);
				//TODO: we might need to package the exception better and give a JSON exception
			}
			
			if(queryParams.containsKey(clientParamName)){
				//this is the request param name that has to be sent
				valueToPass = queryParams.get(clientParamName);
			}
			else if(requestParam.paramType != null && requestParam.paramType.equals(TYPE_DEFAULT) && requestParam.paramValue != null){
				//in case the client param is missing and the current param is of type default use the default value
				valueToPass = new ArrayList<String>();
				valueToPass.add(requestParam.paramValue);
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
		//includes all the results
		target = target.queryParam("q", "*:*");
		System.out.println("Querying " + target.getUri());
		
		Response response = target.request(DEFAULT_MEDIATYPE).get(); //set the accepted media type to one defined
		
		return response.readEntity(String.class);

	}



}
