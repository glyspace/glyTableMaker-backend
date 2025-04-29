package org.glygen.tablemaker.util;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.glygen.tablemaker.view.GlycoproteinView;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class UniProtUtil {

    static String url = "https://www.uniprot.org/uniprot/";
    static String type = ".fasta";
    static String uniSaveUrl = "https://rest.uniprot.org/unisave/";
    
    
    public static String getSequenceFromUniProt(String uniProtId, String version) throws IOException {
    	if (version == null) version = "last";
    	
    	// first retrieve all versions to determine the entry version
    	String historyUrl = uniSaveUrl + uniProtId;
    	String entryVersion = null;
    	
    	try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(historyUrl);
            HttpResponse response = httpClient.execute(request);
            String history = EntityUtils.toString(response.getEntity());
            JSONObject obj = new JSONObject(history);
            JSONArray results = obj.getJSONArray("results");

            
            for (int i = 0; i < results.length(); i++) {
            	JSONObject result = results.getJSONObject(i);
            	if (String.valueOf(result.getInt("sequenceVersion")).equals(version)) {
            		entryVersion =  String.valueOf(result.getInt("entryVersion"));
            		break;
            	}
            }
    	} catch (Exception e) {
    		throw new IOException ("Failed to get protein history details from Uniprot for " + uniProtId + ". Reason: " + e.getMessage());
    	}
            
    	if (entryVersion == null)
        	throw new IOException ("Failed to get protein version details from Uniprot for " + uniProtId + " Could not find entry version matching the sequence version " + version);

    	
    	String requestURL = url + uniProtId + type + "?version=" + entryVersion;
        
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(requestURL);
            HttpResponse response = httpClient.execute(request);
            String fasta = EntityUtils.toString(response.getEntity());
           
            if (fasta == null)
                return null;
            String sequence = fasta.substring(fasta.indexOf("\n")+1);
            sequence = sequence.replaceAll("\n", "");
            return sequence;
        } catch (Exception e) {
        	throw new IOException ("Failed to get protein sequence details from Uniprot for " + uniProtId + " version: " + version + ". Reason: " + e.getMessage());
        }
    }
    
    public static GlycoproteinView getProteinFromUniProt (String uniProtId, String version) throws Exception {
    	
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
            if (obj.has("genes")) {
            	JSONArray arr = obj.getJSONArray("genes"); 
            	JSONObject gene = arr.getJSONObject(0);
            	if (gene.has("geneName")) {
            		JSONObject geneName = gene.getJSONObject("geneName");
            		if (geneName != null) {
            			protein.setGeneSymbol (geneName.getString("value"));
            		}
            	}
            }
    		
            if (obj.has("sequence")) {
	    		JSONObject seq = obj.getJSONObject("sequence");
	    		if (seq != null) {
	    			protein.setSequence (seq.getString("value"));
	    		}
            }
            
            if (obj.has("entryAudit")) {
            	JSONObject entry = obj.getJSONObject("entryAudit");
            	if (entry.has("sequenceVersion")) {
            		protein.setSequenceVersion(entry.getString("sequenceVersion"));
            	}
            }
            
            if (version != null) {
            	try {
            		String sequence = getSequenceFromUniProt(uniProtId, version);
            		protein.setSequence(sequence);
            		protein.setSequenceVersion(version);
            	} catch (Exception e) {
            		// ignore
            	}
            }
            
            return protein;
        } catch (Exception e) {
            throw new IOException ("Failed to get protein details from Uniprot for " + uniProtId + ". Reason: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
    	GlycoproteinView prot;
		try {
			//prot = UniProtUtil.getProteinFromUniProt("P12345-112");
			//System.out.println (prot.getName() + " gene: " + prot.getGeneSymbol() + " seq: " + prot.getSequence());
			
			String sequence = UniProtUtil.getSequenceFromUniProt("P12763", "2");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
    }
}
