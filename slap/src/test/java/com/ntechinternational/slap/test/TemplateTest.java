package com.ntechinternational.slap.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.ntechinternational.slap.Template;

public class TemplateTest {
	Template template = null;
	
	@Before
	public void setup(){
		String itemTemplate = "My name is &Name. I live at &City, &Country. My contact number is &ContactNum";
		this.template = new Template(itemTemplate);
	}

	/**
	 * ensure that the variables in the item template are substituted.
	 */
	@Test
	public void givenATemplate_AllVairablesAreSubstituted() {
		
		Map<String, String> values = new HashMap<String, String>();
		Map<String, String> defaultValues = new HashMap<String, String>();
		
		values.put("&Name", "Ramesh");
		values.put("&City", "Kathmandu");
		
		defaultValues.put("&Country", "Nepal");
		defaultValues.put("&City", "Pokhara");
		
		String output = template.process(values, defaultValues);
		
		//assert that all the variables are properly substituted
		assertFalse(output.contains("&Name"));
		assertFalse(output.contains("&City"));
		assertFalse(output.contains("&Country"));
	}

	/**
	 * This test ensures that when variable substitution is performed
	 * the least priority is given to default variables
	 */
	@Test
	public void givenATemplate_DefaultValuesAreUsedOnlyWhenValueIsNotProvided(){
		Map<String, String> values = new HashMap<String, String>();
		Map<String, String> defaultValues = new HashMap<String, String>();
		
		values.put("&Name", "Ramesh");
		values.put("&City", "Kathmandu");
		
		defaultValues.put("&Country", "Nepal");
		defaultValues.put("&City", "Pokhara");
		
		String output = template.process(values, defaultValues);
		
		//assert that all the default variables are used only when value is missing
		assertFalse(output.contains("Pokhara"));
		assertTrue(output.contains("Kathmandu"));
		assertTrue(output.contains("Nepal"));
	}
	
	/**
	 * When no value either default or responded value is provided no replacement is done
	 */
	@Test
	public void givenNoValueForAVariable_NoSubstitutionIsDone(){
		Map<String, String> values = new HashMap<String, String>();
		values.put("&Name", "Ramesh");
		
		String output = template.process(values, new HashMap<String, String>());
		
		assertTrue(output.contains("&ContactNum"));
		assertTrue(output.contains("&City"));
		assertTrue(output.contains("&Country"));
		assertFalse(output.contains("&Name"));
	}
	
	/**
	 * This test checks that the key value pair from a string is converted to a map by the 
	 * utility class
	 */
	@Test
	public void givenAKeyValueListString_ItIsConvertedToMap(){
		String keyValueList = "Name:Ramesh,City: Kathmandu,School: Higher Secondary School Of Nepal, Grade: 10";
		
		Map<String, String> keyValuePairs = Template.getVariablesFromString(keyValueList);
		
		assertEquals("Ramesh", keyValuePairs.get("Name"));
		assertEquals("Kathmandu", keyValuePairs.get("City"));
		assertEquals("Higher Secondary School Of Nepal", keyValuePairs.get("School"));
		assertEquals("10", keyValuePairs.get("Grade"));
		
		assertNull(keyValuePairs.get("Random"));
	}
}
