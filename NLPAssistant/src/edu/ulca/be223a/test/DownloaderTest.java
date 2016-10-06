package edu.ulca.be223a.test;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import edu.ucla.be223a.fetcher.Downloader;


public class DownloaderTest {
	String url = "https://clinicaltrials.gov/show/NCT00001380?displayxml=true";
	
	@Test
	public void testDownload() {
		Downloader dl = new Downloader(url);
		try {
			String html = dl.getData();
			System.out.println(html);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
