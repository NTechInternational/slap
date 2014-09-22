package com.ntechinternational.slap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XmlParser {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			URI uri = new URI("http://localhost:8080/docs/challengeresponse.xml");

		    System.out.println("uri.getHost() => " + uri.getHost());
		    System.out.println("uri.getPath() => " + uri.getPath());

		    // Most URIs can be converted to true URLs
		    URL url = uri.toURL();
		    
		    // You can easily get input streams from URLs
		    BufferedReader r = new BufferedReader(
		                         new InputStreamReader(url.openStream(), "UTF-8"));
		    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		    DocumentBuilder db = dbf.newDocumentBuilder();
		    Document doc = db.parse(url.openStream());
		    System.out.println("root of xml file " + doc.getDocumentElement().getNodeName());
		    NodeList nodes = doc.getElementsByTagName("doc");
		    for (int i = 0; i < nodes.getLength(); i++) {    	
		    	Node node = nodes.item(i);
					NodeList childNodes = node.getChildNodes();
					for (int j = 0; j < childNodes.getLength(); j++) {
						Node childNode = childNodes.item(j);

						//if (childNode.getNodeType() == Node.ELEMENT_NODE) {

							Element element = (Element) childNode;
							String elemType = element.getNodeName();

							// set the value of Hashmap item based on the type
							String elemName = element.getAttribute("name");
							String elemValue =element.getNodeValue();
							System.out.println("here " + element.toString()+" "+ elemType + " " + elemName
									+ " " + elemValue);
//						}
					}
				}
		    
		    

		    
			 DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			 
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

}
