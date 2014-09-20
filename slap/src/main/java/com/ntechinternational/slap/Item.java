package com.ntechinternational.slap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Item {
	
	@XmlElement
	public long itemID;
	
	@XmlElement
	public String resultTextID;
	
	@XmlElement
	public String resultText;
}
