package com.ntechinternational.slap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Item {
	
	@XmlElement
	public long itemID;
	private HashMap<String,Object> properties = null;
	
	
	@XmlElement
	public String resultTextID;
	
	@XmlElement
	public String resultText;
	
	public Item(long questionID){
		this.questionID = questionID;
		this.properties = new HashMap(<String,Object>);

	}
	
	public getProperties(){
		return properties;
	}
	public getPropertyVal(String propertyName){
		
		if (this.property!=null && this.properties.containsKey(propertyName)))
			return properties.get(propertyName);
	}
}
