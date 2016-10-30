package edu.ucla.be223a.fetcher;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.xml.sax.SAXException;

/**
 * 
 * This class is designed to download individual xml data from clinicaltrials.gov
 * and pre-process into plain text file
 * 
 * @author Yuhua bill Chen
 *
 */
public class TrialTextFetcher {
	private String outputFilePath = null;
	private String trial_web_url = null;
	
	public TrialTextFetcher(int id, String filePath){
		this.trial_web_url = TrialTextFetcher.formatIdUrl(id);
		this.outputFilePath = filePath;
	}
	
	public String getOutputFilePath() {
		return this.outputFilePath;
	}

	public String getWebUrl(){
		return this.trial_web_url;
	}
	
	public static String formatIdUrl(int id){
		String url = String.format("https://clinicaltrials.gov/show/NCT%08d?displayxml=true", id);
		return url;
	}
	
	/**
	 * Fetch the data in a map format
	 * @param fields
	 * @return
	 */
	public Map<String, List<String>>fetch(List<String> fields){
		Downloader dl = new Downloader(trial_web_url);
		Map<String, List<String>> map = null;
		try{
			XMLParser m_parser = new XMLParser(dl.getDataStream());
			m_parser.parse(fields);
		    map = m_parser.getResult();
			if(map == null)
				System.err.println("Result is empty!");
		}
		catch(IOException e1){
			System.err.println("IOException!");
			e1.printStackTrace();
		}
		catch(SAXException e2){
			System.err.println("SAXException!");
			e2.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}	
		return map;
	}
	
	public void outputToFile(){
		
		
	}
		
	/**
	 * Main function takes 2 argument
	 * @param args of the fetcher
	 * 		  1st argument for the study case id
	 * 		  2nd argument for the output file path
	 * 		  3rd and after are the names of subfields need to fetch
	 */
	public static void main(String args[]){
		int study_id = 99999;
		String file_path = "NTC"+study_id+".json";
		List<String> fields = new ArrayList<>();
		if (args.length < 3){
			fields.add("criteria");
			System.out.println("Invalid argument number, default arguments are used!");
		}
		else{	
			study_id = Integer.parseInt(args[0]);
			file_path = args[1];
			for (int i = 2; i < args.length; i++){
				fields.add(args[i]);
			}
		}
		
		// Print out settings
		System.out.print("Study id:"+study_id+" output file:"+file_path+" fields:");
		for (String s:fields)
			System.out.print(s+" ");
		System.out.println("");

		// Init
		TrialTextFetcher fetcher = new TrialTextFetcher(study_id,file_path);
		
		//Fetch the result
		Map<String,List<String>> result = fetcher.fetch(fields);
		
		//Write to json file
		List<String> url_strs = new ArrayList<>();
		url_strs.add(fetcher.getWebUrl());
		result.put("web_url",url_strs);
		
		JSONHelper.jsonEncoder(result, file_path);
		System.out.println("Finished!");
	}
}
