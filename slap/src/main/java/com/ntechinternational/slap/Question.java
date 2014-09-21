package com.ntechinternational.slap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Question {

	@XmlElement
	public long questionID;
	
	public Question(){
		
	}
	
	public Question(long questionID){
		this.questionID = questionID;
	}
}
