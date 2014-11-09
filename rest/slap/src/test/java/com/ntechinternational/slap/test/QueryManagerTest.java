package com.ntechinternational.slap.test;

import static org.junit.Assert.assertTrue;

import org.apache.commons.configuration.ConfigurationException;
import org.junit.Test;

import com.ntechinternational.slap.ConfigurationMap;
import com.ntechinternational.slap.QueryManager;

public class QueryManagerTest {
	QueryManager querier = null;
	
	@Test
	public void givenRowSize_QueryIsLimitedToTheSize(){
		QueryManager querier;
		try {
			querier = new QueryManager().setReturnSetSize(15)
										.setConfig(ConfigurationMap.getConfig())
										.setRequestPath("/test");
			
			querier.query(false);
			
			assertTrue(querier.getQueriedURL().contains("rows=15"));
			
		} catch (ConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void givenRequestPath_QueryIsForwardedToPath(){
		QueryManager querier;
		try {
			querier = new QueryManager().setReturnSetSize(5)
										.setConfig(ConfigurationMap.getConfig())
										.setRequestPath("/test");
			
			querier.query(false);
			
			assertTrue(querier.getQueriedURL().contains("/test"));
			
		} catch (ConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void givenFilterParam_QueryHasCorrectMappedName(){
		QueryManager querier;
		try {
			querier = new QueryManager().setReturnSetSize(15)
										.setConfig(ConfigurationMap.getConfig())
										.setRequestPath("/test")
										.addFilterParam("BusinessModel", "Membership")
										.addFilterParam("itemId", "1013");
			
			querier.query(false);
			
			assertTrue(querier.getQueriedURL().contains("fq=BusinessModel_ss:Membership"));
			assertTrue(querier.getQueriedURL().contains("fq=itemId:1013"));
			
		} catch (ConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
}
