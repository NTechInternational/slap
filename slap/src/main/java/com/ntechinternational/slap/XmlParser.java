package com.ntechinternational.slap;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XmlParser {
	
	final static String NAME_ATTRIBUTE = "name";
	
	/**
	 * This method transforms all nodes identified by documentNode in the XML string and returns a list of
	 * hash map object
	 * @param xmlString the actual XML document (complete)
	 * @param documentNode the nodes that represent the document to be extracted
	 * @param mappingDefinition the param transformation map i.e. change xmlString node name from one to another
	 * @param condition 
	 */
	public static List<Map<String, Object>> transformDoc(String xmlString, String documentNode, Map<String, String> mappingDefinition, String condition){
		
		List<Map<String, Object>> objects = new ArrayList<Map<String, Object>>();
		
		try{
			NodeList docNodes = null;
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		    DocumentBuilder db = dbf.newDocumentBuilder();
		    Document doc = db.parse(new org.xml.sax.InputSource(new StringReader(xmlString)));
		    XPathFactory xPathfactory = XPathFactory.newInstance();
		    XPath xpath = xPathfactory.newXPath();
		    XPathExpression expr = xpath.compile(documentNode);
	    
		    docNodes = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
		    
		    System.out.println("Number of document nodes found: " + docNodes.getLength());
		    
		    for(int index = 0; index < docNodes.getLength(); index++ ){
		    	Map<String, Object> mappedObject = transformNodeToMap(docNodes.item(index),mappingDefinition, condition);
		    	if(mappedObject != null){
		    		objects.add(mappedObject);
		    	}
		    }
		}
		catch(Exception ex){
			//ignoring exceptions for now
			//TODO: handle exceptions properly
			System.err.println(ex.getMessage());
			System.err.println(ex.getStackTrace());
		}
		
		return objects;
	}
	
	/**
	 * transforms a node to hash map
	 */
	private static Map<String, Object> transformNodeToMap(Node nodeToMap,Map<String, String> mappingDefinition, String condition){
		Map<String, Object> mappedObject = new HashMap<String, Object>();
		NodeList childNodes = nodeToMap.getChildNodes();
		for(int index = 0; index < childNodes.getLength(); index++ ){
			if(childNodes.item(index).getNodeType() == Node.ELEMENT_NODE){
				//process only if the node has been mapped
				Node childNode = childNodes.item(index);
				
				//ignore nodes without NAME_ATTRIBUTE node
				Node nodeNameAttr = childNode.getAttributes().getNamedItem(NAME_ATTRIBUTE);
				if(nodeNameAttr != null){
					String nodeName = nodeNameAttr.getNodeValue();
					
					if(mappingDefinition.containsKey(nodeName)){
						String transformedNodeName = mappingDefinition.get(nodeName);
						//check for source
						//String[] eval = condition.split("|");
						//if (nodeName.equalsIgnoreCase(eval[0]) && childNode.getTextContent().equalsIgnoreCase(eval[1])) 
							mappedObject.put(transformedNodeName, childNode.getTextContent());
						//TODO: All nodes are considered as strings, we will need to do proper type casting based on nodes
					}
				}
			}
		}
		
		if(mappedObject.keySet().size() > 0)
			return mappedObject;
		
		return null;
	}


}
