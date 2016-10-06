package edu.ulca.be223a.test;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.xml.sax.SAXException;

import edu.ucla.be223a.fetcher.Downloader;
import edu.ucla.be223a.fetcher.XMLParser;

public class XMLParserTest {
	String url = "https://clinicaltrials.gov/show/NCT00001380?displayxml=true";
	
	String text = "<aa><criteria><textblock>1. Histopathologically confirmed diagnosis of Langerhans cell histiocytosis according to the criteria defined by the Histiocyte Society - Demonstration of CD1a antigenic determinants on the surface of lesional cells (by immunocytology or immunohistology) or Birbeck granules in lesional cells by electron microscopy 2. Considered at risk or low risk according to the following criteria: 1. Multi-system at risk disease, defined as involvement of one or more risk organs (i.e., hematopoietic system, liver, spleen, or lungs) - No single-system lung involvement 2. Multi-system low-risk disease - Multiple organs involved but without involvement of risk organs 3. Single-system disease - Multifocal bone disease (i.e., lesions in 2 or more different bones) - Localized special site involvement, such as CNS-risk lesions with intracranial soft tissue extension or vertebral lesions with intraspinal soft tissue extension - Vault lesions are not regarded as CNS-risk lesions PROTOCOL ENTRY CRITERIA: --Disease Characteristics-- Histologically documented solid tumor potentially expressing mutant ras Stage II/III adenocarcinoma of the lung following surgery or radiotherapy Limited or extensive small cell lung cancer in complete remission Dukes' C colorectal cancer following appropriate adjuvant chemotherapy Fully resected recurrent colorectal carcinoma Fully resected pancreatic carcinoma Tumor tissue available for determination of ras mutation Paraffin block or fresh tissue Specific point mutation in codon 12 required, which includes: Glycine to cysteine Glycine to aspartic acid Glycine to valine Tumor tissue available for preparation of a tumor cell line and tumor or lymph node tissue for expansion of tumor infiltrating lymphocytes for in vitro laboratory studies preferred No history of CNS metastases --Prior/Concurrent Therapy-- Biologic therapy: At least 4 weeks since prior immunotherapy and recovered Chemotherapy: See Disease Characteristics At least 4 weeks since prior chemotherapy and recovered Endocrine therapy: At least 4 weeks since prior steroids and recovered Radiotherapy: At least 4 weeks since prior radiotherapy and recovered Surgery: See Disease Characteristics Not specified --Patient Characteristics-- Age: Over 18 Performance status: ECOG 0-1 Life expectancy: More than 3 months Hematopoietic: WBC at least 3,000/mm3 Lymphocyte count at least 600/mm3 Platelet count at least 100,000/mm3 Hepatic: Bilirubin no greater than 2.0 mg/dL ALT and AST no greater than 4 times normal Hepatitis B and C surface antigen negative Renal: Creatinine no greater than 2.0 mg/dL Cardiovascular: No active ischemic heart disease (New York Heart Association class III/IV) No myocardial infarction within 6 months No history of arrhythmia No clinical symptoms suggesting cardiac insufficiency Pulmonary: No clinical symptoms suggesting pulmonary insufficiency Immunologic: Responsive to anergy skin testing with mumps, trichophyton, or candida antigens HIV negative No autoimmune disease, e.g.: Systemic lupus erythematosus Multiple sclerosis Ankylosing spondylitis Other: No active infection requiring antibiotics No history of malignancy except curatively treated basal cell skin carcinoma or curatively treated carcinoma in situ of the cervix Not pregnant or nursing Negative pregnancy test Fertile patients must use effective contraception"+
"</textblock></criteria></aa>";
	@Test
	public void testParsing() throws ParserConfigurationException {
		List<String> m_fields = new ArrayList<String>();
		m_fields.add("criteria");
		m_fields.add("link_text");
		Downloader dl = new Downloader(url);
		try{
			File f = new File("NCT00001380.xml");
			//XMLParser m_parser = new XMLParser(new ByteArrayInputStream(text.getBytes()));
			//XMLParser m_parser = new XMLParser(new FileInputStream(f));
			XMLParser m_parser = new XMLParser(dl.getDataStream());
			m_parser.parse(m_fields);
			Map<String, List<String>> map = m_parser.getResult();
			if(map != null)
				for (String k : map.keySet()){
					System.out.println(k+":"+map.get(k));
				}
			else
				fail("Map is null!");
		}
		catch(IOException e1){
			System.err.println("IOException!");
			e1.printStackTrace();
		}
		catch(SAXException e2){
			System.err.println("SAXException!");
			e2.printStackTrace();
		}

	}

}
