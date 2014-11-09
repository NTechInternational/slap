package com.ntechinternational.slap;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class JsonParser {

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
			BufferedReader r = new BufferedReader(
					new InputStreamReader(url.openStream(), "UTF-8"));

			JSONParser jsonParser = new JSONParser();
			JSONObject jsonObject = (JSONObject) jsonParser.parse(r);

			// get a String from the JSON object
			String doc = (String) jsonObject.get("doc");
			System.out.println("The doc name is: " + doc);



		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (ParseException ex) {
			ex.printStackTrace();
		} catch (NullPointerException ex) {
			ex.printStackTrace();
		} catch (java.net.URISyntaxException ex){
			ex.printStackTrace();
		}

	}
}