package com.ntechinternational.slap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Question {

	@XmlElement(name="questionID")
	public long id;
	
	@XmlElement
	public String businessModel;
	
	public Question(){
		
	}
	
	public Question(long questionID){
		this.id = questionID;
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
