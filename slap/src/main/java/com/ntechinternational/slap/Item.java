package com.ntechinternational.slap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Item {
	
	@XmlElement(name="itemID")
	public long id;
	
	@XmlElement
	public String resultTextID;
	
	@XmlElement
	public String resultText;
	
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
