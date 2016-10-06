package edu.ucla.be223a.fetcher;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Class to parse plain text xml and filter out the text we need
 * @author bill
 *
 */
public class XMLParser {
	private InputStream is = null;
	private SAXParser parser = null;
	private List<String> subfields = new ArrayList<>();
	private SAXHandler m_hdl = null; // Handler for the xml
	
	public XMLParser(InputStream is) throws ParserConfigurationException, SAXException{
		this.is = is;
		this.parser = SAXParserFactory.newInstance().newSAXParser();
	}
	
	public Map<String,List<String>> parse(List<String> subfields) throws SAXException, IOException{
		this.subfields.addAll(subfields);
		this.m_hdl = new SAXHandler(this.subfields);
		this.parser.parse(is, m_hdl);
		return this.m_hdl.getResult();
	}
	
	public Map<String, List<String>> getResult(){
		if (m_hdl == null)
			return null;
		return this.m_hdl.getResult();
	}
}

/**
 * Handler for the xml parser
 * To extract the text block from a subfield of clinical trial xml
 * for example, criteria
 * @author bill
 *
 */
class SAXHandler extends DefaultHandler{
	private List<String> subfields = null;
	// A stack of string to store the content string
	// Since we may have interested fields in nested level
	// we need a stack to remember not only the value, but also the position to keep track
	private Map<Integer, String[]> level_value = new HashMap<>();
	private int current_level = 0;
	private Map<String,List<String>> name_val = new HashMap<>();
	
	public SAXHandler(List<String> subfields){
		this.subfields = subfields;
	}
	
	@Override
	public void startElement(String uri, String localName,
            String qName, Attributes attributes)
            throws SAXException {
		if ("textblock".equals(qName))
			return;
		current_level++;
		if (this.subfields.contains(qName)){
			String[] pair = new String[2];
			pair[0] = qName;
			pair[1] = null;
			level_value.put(current_level,pair);				
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) 
			throws SAXException {
		String value = String.copyValueOf(ch, start, length).trim();
		if (level_value.containsKey(current_level)){
			// Hit
			String[] pair = level_value.get(current_level);
			if (pair[1] == null)
				pair[1] = value;
			else
				pair[1] = pair[1] + value;
			level_value.put(current_level, pair);
		}
	}
	
	@Override  
	public void endElement(String uri, String localName,
            String qName) throws SAXException {
		if ("textblock".equals(qName))
			return;
		if (level_value.containsKey(current_level)){
			String[] pair = level_value.get(current_level);
			String value = pair[1];
			List<String> l_str;
			if (name_val.containsKey(qName)){ 
				l_str = name_val.get(qName);
				l_str.add(value);
			}
			else{
				l_str = new ArrayList<String>();
				l_str.add(value);
			}
			level_value.remove(current_level);
			name_val.put(qName, l_str);
		}
		current_level--;		
	}
	
	/**
	 * Get the final result
	 * @return Name and content value pairs
	 */
	public Map<String,List<String>> getResult(){
		return this.name_val;
	}
}