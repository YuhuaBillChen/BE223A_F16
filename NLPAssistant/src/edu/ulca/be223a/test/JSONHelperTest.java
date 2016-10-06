package edu.ulca.be223a.test;

import static org.junit.Assert.*;

import edu.ucla.be223a.fetcher.JSONHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class JSONHelperTest {
	Map<String,List<String>> m = new HashMap<>();

    @Before
    public void init(){
      List<String> str1 = new ArrayList<>();
      List<String> str2 = new ArrayList<>();
      str1.add("1");
      str2.add("2");
      str2.add("3");
      m.put("a", str1);
      m.put("b", str2);
    }
	
	@Test
	public void testJsonEncoder() {
		JSONHelper.jsonEncoder(m, "test.json");
	}

	@Test
	public void testJsonDecoder() {
		Map<String,List<String>> map = JSONHelper.jsonDecoder("test.json");
		assertEquals("1",map.get("a").get(0));
		assertEquals("2",map.get("b").get(0));
		assertEquals("3",map.get("b").get(1));
		
	}

}
