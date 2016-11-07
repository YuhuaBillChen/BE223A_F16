package edu.ucla.be223a.gatemsa;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import gate.GateBatch;
import driver.GenMSADriver;
/**
 * Launcher for GateBatch
 * @author bill
 *
 */
public class GateBatchLauncher {
	String gbPropsUrl = null;
	GateBatch gb = null;
	Properties props = new Properties();
	
	/**
	 * Default constructor
	 * @param gbPropsUrl properties files for gatebatch
	 */
	public GateBatchLauncher(String gbPropsUrl){
		this.gbPropsUrl = gbPropsUrl;
		gb = new GateBatch();
	}
	
	/**
	 * Initialization process
	 * @throws IOException
	 */
	public void init() throws IOException{
		InputStream in = new FileInputStream(this.gbPropsUrl);
		props.load(in);
		in.close();
		gb.init(props);				
	}
	
	/**
	 * Run the GATE process
	 */
	public void run(){
		String dbuser = props.getProperty("dbUser"), dbpwd = props.getProperty("dbPassword"), docUser =  
				props.getProperty("docUser"), docPwd = props.getProperty("docPassword");
		gb.process(dbuser,dbpwd,docUser,docPwd);
	}
	
	
	public static void main(String[] args){
		GateBatchLauncher gbLauncher = new GateBatchLauncher("resources/gate-batch-223a-remote.properties");
		try {
			gbLauncher.init();
		} catch (IOException e) {
			e.printStackTrace();
		}
		gbLauncher.run();
		return;
	}
}
