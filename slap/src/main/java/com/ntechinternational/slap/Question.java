package com.ntechinternational.slap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.HashMap;

@XmlRootElement
public class Question {

	@XmlElement
	public long id;
	public String businessModel;
	private HashMap<String,Object> properties = null;
	
	public Question(){

		this.properties = new HashMap<String,Object>();
		
	}
	
	public Question(long questionID){
		this.properties = new HashMap<String,Object>();

		this.id = questionID;
	}
	
	public HashMap<String,Object> getProperties(){
		return properties;
	}
	public Object getPropertyVal(String propertyName){
		
		if (this.properties!=null && this.properties.containsKey(propertyName))
			return properties.get(propertyName);
		
		return null;
	}
	public Question put(String attributeName, String value){
		if(attributeName.equals("businessModel")){
			this.businessModel = value;
		}
		else if(attributeName.equals("id")){
			this.id = Long.parseLong(value);
		}
		else{
			System.err.println("Ignoring invalid parameter of type " + attributeName);
		}
		
		return this;

	}
}
