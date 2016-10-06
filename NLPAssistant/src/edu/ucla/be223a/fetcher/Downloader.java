package edu.ucla.be223a.fetcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

public class Downloader{
	private String url;
	
	public Downloader(String url){
		this.url = url;
	}
	
	/**
	 * Download data from this url
	 * @return html plain text
	 * @throws IOException 
	 */
	public String getData() throws IOException{
		StringBuilder sb = new StringBuilder();
		URL url_obj = new URL(url);
		InputStream is = url_obj.openStream();
		BufferedReader br  = new BufferedReader(new InputStreamReader(is));
		String line = null;
		// Read the html line by line
		while( (line = br.readLine()) != null){
			sb.append(line+"\r\n");
		}
		br.close();
		return sb.toString();
	}
	
	/**
	 * Get the input stream from this url
	 * @return InputStream of the xml
	 * @throws IOException
	 */
	public InputStream getDataStream() throws IOException{
		URL url_obj = new URL(url);
		InputStream is = url_obj.openStream();
		return is;	
	}
}