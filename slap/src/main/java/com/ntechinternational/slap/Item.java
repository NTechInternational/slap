package com.ntechinternational.slap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.HashMap;

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
		this.itemID = itemID;
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
}
