package com.ntechinternational.slap.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.ntechinternational.slap.SubstitutionSummary;
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
		List<SubstitutionSummary> changeSummary = new ArrayList<SubstitutionSummary>();
		
		values.put("&Name", "Ramesh");
		values.put("&City", "Kathmandu");
		
		defaultValues.put("&Country", "Nepal");
		defaultValues.put("&City", "Pokhara");
		
		String output = template.process(values, defaultValues, changeSummary);
		
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
		List<SubstitutionSummary> changeSummary = new ArrayList<SubstitutionSummary>();
		
		values.put("&Name", "Ramesh");
		values.put("&City", "Kathmandu");
		
		defaultValues.put("&Country", "Nepal");
		defaultValues.put("&City", "Pokhara");
		
		String output = template.process(values, defaultValues, changeSummary);
		
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
		List<SubstitutionSummary> changeSummary = new ArrayList<SubstitutionSummary>();
		
		values.put("&Name", "Ramesh");
		
		String output = template.process(values, new HashMap<String, String>(), changeSummary);
		
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
	
	@Test
	public void givenImproperlyCasedString_CaseIsCorrected(){
		String itemTemplate = "my sentence is incorrect.   i need it corrected.";
		List<SubstitutionSummary> changeSummary = new ArrayList<SubstitutionSummary>();
		
		template = new Template(itemTemplate);
		
		Map<String, String> values = new HashMap<String, String>();
		Map<String, String> defaultValues = new HashMap<String, String>();
		
		String output = template.process(values, defaultValues, changeSummary);
		
		assertEquals("My sentence is incorrect. I need it corrected.", output);
	}
	
	@Test
	public void issueWithItem1013TestedWithSeparator(){
		String itemTemplate = "Introduce a new &Customer and &RewardAction &RewardAmount &Reward.";
		List<SubstitutionSummary> changeSummary = new ArrayList<SubstitutionSummary>();
		Map<String, String> variables = Template.getVariablesFromString("&Customer:Member, &RewardAction:get, &RewardAmount:one, &Reward:month free (Introduce a new member and get one month free.)");
		
		template = new Template(itemTemplate);
		String output = template.process(variables, new HashMap<String, String>(), changeSummary);
		
		assertFalse(output.contains("&RewardAction"));
		
	}
	
	@Test
	public void issueWithRemovalOfSpace(){
		String itemTemplate = "[Beginning &StartTime and ending] [by &Deadline] &OfferAction &OfferingAmount &Offering [and &Activity,] and &Reward] [to &Beneficiary].";
		Map<String, String> variables = Template.getVariablesFromString("&OfferAction:buy, &OfferingAmount:one, &Offering:product, &Reward:get a second one free (Buy one product and get a second one free.)");
		List<SubstitutionSummary> changeSummary = new ArrayList<SubstitutionSummary>();
		
		template = new Template(itemTemplate);
		String output = template.process(variables, new HashMap<String, String>(), changeSummary);
		
		assertEquals("Buy one product and get a second one free (Buy one product and get a second one free.).",
				output);
		
	}
}
