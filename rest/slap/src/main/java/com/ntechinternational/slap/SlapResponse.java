package com.ntechinternational.slap;



import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@XmlRootElement
public class SlapResponse {
	
	
	@XmlElement
	public String visitorId;
	
	@XmlElementWrapper
	@XmlElement(name = "question", nillable = true)
	@JsonInclude(Include.NON_NULL)
	public List<Map<String, Object>> questions;
	
	@XmlElementWrapper
	@XmlElement(name = "item", nillable = true)
	@JsonInclude(Include.NON_NULL)
	public List<Map<String, Object>> items;
	
	@XmlElement
	@JsonInclude(Include.NON_NULL)
	public String errorDescription;
	
	
	static final String CONFIG_FILE = "Map.xml";
	
	public SlapResponse(){
		
	}
	
}
