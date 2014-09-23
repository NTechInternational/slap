package com.ntechinternational.slap;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class XmlParser {

	public static NodeList getNodes(String xmlString, String xPathExp) {
		
		NodeList nodes = null;
		
		try{
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		    DocumentBuilder db = dbf.newDocumentBuilder();
		    Document doc = db.parse(new org.xml.sax.InputSource(new StringReader(xmlString)));
		    XPathFactory xPathfactory = XPathFactory.newInstance();
		    XPath xpath = xPathfactory.newXPath();
		    XPathExpression expr = xpath.compile(xPathExp);
		    
		    nodes = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
		}
		catch(Exception ex){
			//ignoring exceptions for now
			//TODO: handle exceptions properly
		}
		
		return nodes;
	    
	}

}
