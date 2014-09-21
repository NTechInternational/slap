package com.ntechinternational.slap;


import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

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
}
