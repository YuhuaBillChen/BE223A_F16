package edu.ucla.be223a.fetcher;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class JSONHelper {

	/**
	 * Decode the json file into a map whose key is the field name and values
	 * are a list of strings occur in the trail
	 * 
	 * @param filepath
	 * @return a hashmap of result
	 */
	public static Map<String, List<String>> jsonDecoder(String filepath) {
		Map<String, List<String>> result = new HashMap<>();
		JSONParser parser = new JSONParser();
		JSONObject obj = null;

		InputStreamReader rd = null;
		try {
			rd = new InputStreamReader(new FileInputStream(new File(filepath)));
			obj = (JSONObject) parser.parse(rd);
			for (Object s : (obj.keySet())) {
				result.put((String) s, (List<String>) obj.get(s));
			}
			rd.close();
		} catch (FileNotFoundException e) {
			System.err.println("File not found!:" + filepath);
			e.printStackTrace();
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Encode the result map into a json format string and save it into file
	 * 
	 * @param result map of result
	 * @param output_filepath file location to save
	 */
	public static void jsonEncoder(Map<String, List<String>> result, String output_filepath) {
		JSONObject obj = new JSONObject();
		StringWriter out = new StringWriter();
		for (String s : result.keySet()) {
			obj.put(s, result.get(s));
		}
		try {
			obj.writeJSONString(out);
		} catch (IOException e) {
			e.printStackTrace();
		}

		String jsonText = out.toString();

		File f = new File(output_filepath);

		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(f));
			writer.write(jsonText);
			writer.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return;
	}
}
