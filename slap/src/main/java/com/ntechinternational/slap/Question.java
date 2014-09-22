package com.ntechinternational.slap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Question {

	@XmlElement
	public long questionID;
	private HashMap<String,Object> properties = null;
	
	public Question(){
		this.properties = new HashMap(<String,Object>);
		
	}
	
	public Question(long questionID){
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
