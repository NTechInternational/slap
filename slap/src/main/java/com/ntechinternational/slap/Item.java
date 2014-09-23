package com.ntechinternational.slap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.HashMap;

@XmlRootElement
public class Item {
	
	@XmlElement(name="itemID")
	public long id;
	
	@XmlElement
	public String resultTextID;
	
	@XmlElement
	public String resultText;
	
	HashMap<String,Object> properties;
	
	public Item(){

		this.properties = new HashMap<String,Object>();
		
	}
	public Item(long itemID){
		this.id = itemID;
		this.properties = new HashMap<String,Object>();

	}
	
	public HashMap<String,Object> getProperties(){
		return properties;
	}
	public Object getPropertyVal(String propertyName){
		if (this.properties!=null && this.properties.containsKey(propertyName))
			return properties.get(propertyName);
		
		return null;
	}
	
	public Item put(String attributeName, String value){
		//TODO: dynamically assign value to an attribute
		if(attributeName.equals("resultTextID")){
			this.resultTextID = value;
		}
		else if(attributeName.equals("id")){
			this.id = Long.parseLong(value);
		}
		else if(attributeName.equals("resultText")){
			this.resultText = value;
		}
		else{
			System.err.println("Ignoring invalid parameter of type " + attributeName);
		}

		return this;
	}
}
