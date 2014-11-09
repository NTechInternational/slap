package com.ntechinternational.slap.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.ntechinternational.slap.XmlParser;

public class XmlParserTest {

	@Test
	public void givenXMLMapping_CorrectElementIsReturned(){
		final String xmlString = "<docs>"
								 + "<doc><int name='id'>1</int><str name='st'>String</str><unmapped name='a'>Asd</unmapped></doc>"
								 + "<doc><int name='id'>2</int><str name='st'>String 2</str><unmapped name='a'>Asd</unmapped></doc>"
								 + "<doc><int name='id'>3</int><str name='st'>String 3</str><unmapped name='a'>Asd</unmapped></doc>"
								 + "<doc><unmappedid>4</unmappedid><unmapped>Asd</unmapped></doc>"
								 + "</docs>";
		final String documentNode = "/docs/doc";
		final Map<String, String> mappingDefinition = new HashMap<String, String>();
		
		mappingDefinition.put("id", "MappedId");
		mappingDefinition.put("st", "Name");
		
		List<Map<String, Object>> allDocs = XmlParser.transformDoc(xmlString, documentNode, mappingDefinition, "");
		
		assertEquals(3, allDocs.size()); //after the transformation we should have 3 documents
		assertFalse(allDocs.get(0).containsKey("unmapped")); //unmapped field is skipped
		
		assertTrue(allDocs.get(0).containsKey("MappedId")); //ensures that id is converted to mapped id
		assertEquals("1", allDocs.get(0).get("MappedId")); //ensures that the correct value is set.
		
	}
}
