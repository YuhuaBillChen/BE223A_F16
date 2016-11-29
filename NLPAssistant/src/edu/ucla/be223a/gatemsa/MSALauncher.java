package edu.ucla.be223a.gatemsa;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import msa.GenMSADriver;

public class MSALauncher {
	GenMSADriver msa_driver;
	String gpPropsUrl;
	Properties props = new Properties();
	String dbUser, dbPwd;
	
	public MSALauncher(String gbProposUrl){
		this.gpPropsUrl = gbProposUrl;
		msa_driver = new GenMSADriver();
	}
	
	public void init() throws IOException{
		InputStream in = new FileInputStream(this.gpPropsUrl);
		props.load(in);
		in.close();		
		dbUser = props.getProperty("docDBUser");
		dbPwd = props.getProperty("docDBPassword");
		msa_driver.init(props);
	}
	
	public void run(){
		msa_driver.run(dbUser, dbPwd, dbUser,dbPwd);
	}
	public static void main(String[] args){
		MSALauncher msa = new MSALauncher("resources/gen-msa-remote.properties");
		try {
			msa.init();
			msa.run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}