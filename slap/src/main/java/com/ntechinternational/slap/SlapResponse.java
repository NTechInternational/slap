package com.ntechinternational.slap;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@XmlRootElement
public class SlapResponse {
	
	@XmlElement
	public long visitorId;
	
	@XmlElementWrapper
	@XmlElement(name = "question", nillable = true)
	@JsonInclude(Include.NON_NULL)
	public List<Question> questions;
	
	@XmlElementWrapper
	@XmlElement(name = "item", nillable = true)
	@JsonInclude(Include.NON_NULL)
	public List<Item> items;
	
	
	static final String CONFIG_FILE = "Map.xml";
	
	private XMLConfiguration config = null;
	public SlapResponse(){
		try{
			this.config = new XMLConfiguration(CONFIG_FILE); //load the configuration file
			//TODO: we might want to load the configuration file only once
			//move it to a bean with application scope.
		}
		catch(ConfigurationException ex){
			
		}
	}
	
	
	/**
	 * queries a given URL in the configuration file and returns the result
	 * @param resourcePath the location of the path in the URL
	 * @param mediaType the type of accepted media JSON or XML
	 * @param extraParams any param beside the configuration that is to be sent in the query
	 */
	public String query(String resourcePath, String mediaType, MultivaluedMap<String, String> extraParams){
		
		Client client = ClientBuilder.newClient();
		WebTarget target = client.target(config.getString("request.url")).path(resourcePath);
		MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<String, String>();
		
		//add the params passed
		if(extraParams != null){
			for(String key : extraParams.keySet()){
				queryParams.addAll(key, extraParams.get(key));
			}
		}
		
		//add required params to query param
		List<HierarchicalConfiguration> paramConfigs = config.configurationsAt("request.requiredParam.param");
		
		//add other params to query param too 
		paramConfigs.addAll(config.configurationsAt("request.otherParam.param"));
		
		for(HierarchicalConfiguration conf : paramConfigs){
			String paramName = conf.getString("[@name]");
			String paramValue = conf.getString("[@value]"); //get the value from the passed list
			
			//if no valid value is found we skip the param
			if(paramValue == null){
				continue;
			}
			
			queryParams.putSingle(paramName, paramValue);
			target = target.queryParam(paramName, paramValue);
		}
		
				
		System.out.println(target.getUri());

		Response response = target.request(mediaType).get(); //set the accepted media type to one defined
	
		return response.readEntity(String.class);
	}



	public static SlapResponse createResponse(){
		SlapResponse response = new SlapResponse();
		
		//query the questions server
		//TODO: add support for JSON request
		String questionResponse = response.query(response.config.getString("request.questionPath"), MediaType.APPLICATION_XML, null);
		
		//TODO: since question and answer requests could be performed simultaneously, change to async method. (Parallelize)
		String answerResponse = response.query(response.config.getString("request.challengePath"), MediaType.APPLICATION_XML, null);
		
		response.mergeResponse(questionResponse, answerResponse);
		
		return response;
	}


	private void mergeResponse(String questionResponse, String answerResponse) {
		NodeList questionNodes = XmlParser.getNodes(questionResponse, "/response/result/doc");
		NodeList itemNodes = XmlParser.getNodes(answerResponse, "/response/result/doc");
		final String NAME_ATTRIBUTE = "name";
		
		Map<String, String> mappingConfigs = new HashMap<String, String>();
		for(HierarchicalConfiguration mapConfig : config.configurationsAt("response.mappings.fieldName")){
			mappingConfigs.put(mapConfig.getString("[@source]"), mapConfig.getString("[@target]"));
		}
		
		System.out.println("Merging the response");
		
		if(questionNodes != null){
			System.out.println("Found some questions");
			this.questions = new ArrayList<Question>();
			for(int index = 0; index < questionNodes.getLength(); index++){
				System.out.println("Adding question " + index);
				Question questionItem = new Question();
				boolean hasAValueSet = false;
				Node question  = questionNodes.item(index);
				NodeList questionChildNodes = question.getChildNodes(); //assuming that information is present only in 1st level
				//the assumption may not be completely valid.
				for(int childIndex = 0 ; childIndex < questionChildNodes.getLength(); childIndex++){
					Node childNode = questionChildNodes.item(childIndex);
					String childNodeName = childNode.getAttributes().getNamedItem(NAME_ATTRIBUTE).getNodeValue();
					System.out.println(childNodeName);
					
					if(mappingConfigs.containsKey(childNodeName)){
						hasAValueSet = true;
						questionItem.put(mappingConfigs.get(childNodeName), childNode.getTextContent());
					}
				}
				
				if(hasAValueSet){
					this.questions.add(questionItem);
				}
			}
		}
		
		if(itemNodes != null){
			System.out.println("Found some items");
			this.items = new ArrayList<Item>();
			for(int index = 0; index < itemNodes.getLength(); index++){
				System.out.println("Adding item " + index);
				Item challengeItem = new Item();
				boolean hasAValueSet = false;
				Node item  = itemNodes.item(index);
				NodeList itemChildNodes = item.getChildNodes(); //assuming that information is present only in 1st level
				//the assumption may not be completely valid.
				for(int childIndex = 0 ; childIndex < itemChildNodes.getLength(); childIndex++){
					Node childNode = itemChildNodes.item(childIndex);
					String childNodeName = childNode.getAttributes().getNamedItem(NAME_ATTRIBUTE).getNodeValue();
					System.out.println(childNodeName);
					
					if(mappingConfigs.containsKey(childNodeName)){
						hasAValueSet = true;
						challengeItem.put(mappingConfigs.get(childNodeName), childNode.getTextContent());
					}
				}
				
				if(hasAValueSet){
					this.items.add(challengeItem);
				}
			}
		}
		
	}
}
