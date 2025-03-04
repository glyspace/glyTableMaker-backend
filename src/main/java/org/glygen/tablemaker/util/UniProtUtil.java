package org.glygen.tablemaker.util;

import java.io.IOException;

import org.glygen.tablemaker.view.GlycoproteinView;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class UniProtUtil {

    static String url = "https://www.uniprot.org/uniprot/";
    static String type = ".fasta";
    
    public static String getSequenceFromUniProt(String uniProtId) {
        RestTemplate restTemplate = new RestTemplate();
        String requestURL = url + uniProtId + type;
        try {
            ResponseEntity<String> response = restTemplate.exchange(requestURL, HttpMethod.GET, null, String.class);
            String fasta = response.getBody();
            if (fasta == null)
                return null;
            String sequence = fasta.substring(fasta.indexOf("\n")+1);
            sequence = sequence.replaceAll("\n", "");
            return sequence;
        } catch (Exception e) {
            return null;
        }
    }
    
    public static GlycoproteinView getProteinFromUniProt (String uniProtId) throws Exception {
    	
    	RestTemplate restTemplate = new RestTemplate();
        String requestURL = url + uniProtId + ".json";
        try {
            ResponseEntity<String> response = restTemplate.exchange(requestURL, HttpMethod.GET, null, String.class);
            String details = response.getBody();
            if (details == null)
                return null;
            GlycoproteinView protein = new GlycoproteinView ();
            protein.setUniprotId(uniProtId);
            
            JSONObject obj = new JSONObject(details);
            JSONObject description = obj.getJSONObject("proteinDescription");
            if (description != null) {
            	if (description.has("recommendedName")) {
	            	JSONObject rec = description.getJSONObject("recommendedName");
	            	if (rec != null) {
	            		JSONObject fullName = rec.getJSONObject("fullName");
	            		protein.setProteinName(fullName.getString("value"));
	            	}
            	} else if (description.has("submissionNames")) {
            		JSONArray names = description.getJSONArray("submissionNames");
            		JSONObject name = names.getJSONObject(0);
            		if (name != null) {
            			JSONObject fullName = name.getJSONObject("fullName");
	            		protein.setProteinName(fullName.getString("value"));
            		}
            	}
            		
            }
            
    		JSONArray arr = obj.getJSONArray("genes"); 
    		JSONObject gene = arr.getJSONObject(0);
    		JSONObject geneName = gene.getJSONObject("geneName");
    		if (geneName != null) {
    			protein.setGeneSymbol (geneName.getString("value"));
    		}
    		
    		JSONObject seq = obj.getJSONObject("sequence");
    		if (seq != null) {
    			protein.setSequence (seq.getString("value"));
    		}
            
            return protein;
        } catch (Exception e) {
            throw new IOException ("Failed to get protein details from Uniprot. Reason: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
    	GlycoproteinView prot;
		try {
			prot = UniProtUtil.getProteinFromUniProt("P12345-112");
			System.out.println (prot.getName() + " gene: " + prot.getGeneSymbol() + " seq: " + prot.getSequence());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
    }
}
