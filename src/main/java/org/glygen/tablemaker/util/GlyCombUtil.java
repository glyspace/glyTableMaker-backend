package org.glygen.tablemaker.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.glygen.tablemaker.persistence.glycan.Glycan;
import org.glygen.tablemaker.persistence.protein.GlycanInSite;
import org.glygen.tablemaker.persistence.protein.Glycoprotein;
import org.glygen.tablemaker.persistence.protein.GlycoproteinSiteType;
import org.glygen.tablemaker.persistence.protein.Position;
import org.glygen.tablemaker.persistence.protein.Site;
import org.glygen.tablemaker.persistence.protein.SitePosition;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.util.UriComponentsBuilder;

public class GlyCombUtil {
	
	String apiKey;
	
	static GlyCombUtil instance;
	
	static final String validationURL = "https://glycomb.glycosmos.org/api/input-validation";
	static final String registrationRequestURL = "https://glycomb.glycosmos.org/auth/api/registration-request";
	static final String publishURL = "https://glycomb.glycosmos.org/auth/api/publish-entry";
	
	private GlyCombUtil() {
	}
	
	public static GlyCombUtil getInstance () {
		if (instance == null)
			instance = new GlyCombUtil();
		return instance;
	}
		
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}
	
	public Map<String, Boolean> requestRegistration (Glycoprotein... gps) throws IOException {
		Map<String, Boolean> result = new HashMap<String, Boolean>();
		for (Glycoprotein gp: gps) {
			String input = generateTSV(gp);
			
			if (input == null) {
				result.put(gp.getUniprotId(), false);
				continue;
			}
			
			// step 1 - validation
			HttpEntity multipart = MultipartEntityBuilder.create()
	                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
	                .addPart("uploaded_file", new StringBody(input, ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8)))
	                .build();
	
	
			HttpPost post = new HttpPost(validationURL);
	        post.setEntity(multipart);
	
	
			try (CloseableHttpClient client = HttpClients.createDefault();
	             CloseableHttpResponse response = client.execute(post)) {
	
	            int status = response.getStatusLine().getStatusCode();
	            String body = response.getEntity() != null
	                    ? EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8)
	                    : "";
	            if (status < 200 || status >= 300) {
	                throw new RuntimeException("Upload failed: " + status + " " + body);
	            }
	            
	            String rawData = null;
	            // check for errors
	            final JSONObject obj = new JSONObject(body);
	    		if (obj.has("contents")) {
	    			JSONArray contents = obj.getJSONArray("contents");
	    			if (contents.length() > 0) {
	    				JSONObject content = contents.getJSONObject(0);
	    				rawData = content.getString("raw_data");
	    				int errorCount = content.getInt("error_count");
	    				if (errorCount > 0) {
	    					result.put(gp.getUniprotId(), false); // validation failed, cannot register
	    					continue;
	    				}	
	    			}
	    		}
	    		
	            // step 2 - registration request
	            // get raw_data from response and use in registration request
	    		String url = UriComponentsBuilder.fromHttpUrl(registrationRequestURL)
	                    .queryParam("datatype", "glycoprotein")
	                    .queryParam("raw_data", rawData)
	                    .toUriString();
	    		
	    		
	    		HttpPost get = new HttpPost(url);
	    		get.addHeader("Authorization", "Bearer " + apiKey);
	    		get.addHeader("Content-Type",  MediaType.APPLICATION_FORM_URLENCODED.toString());
	    		
	    		HttpResponse response2 = client.execute(get);
	    		status = response2.getStatusLine().getStatusCode();
	            body = response2.getEntity() != null
	                    ? EntityUtils.toString(response2.getEntity(), StandardCharsets.UTF_8)
	                    : "";
	            if (status < 200 || status >= 300) {
	                throw new IOException("Registration request failed: " + status + " " + body);
	            }
	            
	            final JSONObject content = new JSONObject(body);
	            String receiptNumber = null;
	            if (content.has("contents")) {
	            	//set the gp glycomb receipt number
	            	receiptNumber = content.getString("contents");
	            	result.put(gp.getUniprotId(), true);
	            	gp.setGlycombReceiptNumber(receiptNumber);
	            }
	            else {
	                throw new IOException("Registration request not accepted. Receipt number not found " + status + " " + body);
	            }
	            
	            // check if accession number is available for the receipt number
	            // if so, publish 
	            
	            
			}	
		}
		
		return result;
	}
	
	public void publish(Glycoprotein... glycoproteins) throws IOException {
		String[] receiptNumbers = new String[glycoproteins.length];
		int i=0;
		for (Glycoprotein gp: glycoproteins) {
			receiptNumbers[i++] = gp.getGlycombReceiptNumber();
		}
		String url = UriComponentsBuilder.fromHttpUrl(publishURL)
                .queryParam("reception_numbers", "[\"" + String.join("\", \"", receiptNumbers) + "\"]")
                .toUriString();
		HttpPost post = new HttpPost(url);
		post.addHeader("Authorization", "Bearer " + apiKey);
		post.addHeader("Content-Type",  MediaType.APPLICATION_FORM_URLENCODED.toString());
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			HttpResponse response = client.execute(post);
			int status = response.getStatusLine().getStatusCode();
	        String body = response.getEntity() != null
	                ? EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8)
	                : "";
	        if (status < 200 || status >= 300) {
	            throw new IOException("Registration publish failed: " + status + " " + body);
	        }
	        
	        // get accession numbers
	        final JSONObject obj = new JSONObject(body);
    		if (obj.has("contents")) {
    			JSONArray contents = obj.getJSONArray("contents");
    			if (glycoproteins.length == contents.length()) {
	    			for (i=0; i < contents.length(); i++) {
	    				JSONObject content = contents.getJSONObject(i);
	    				String accessionNo = content.getString("accession");
	    				glycoproteins[i].setGlycombAccessionNumber(accessionNo);
	    			}
	    		} else {
	    			throw new IOException ("Could not receive accession numbers for all submissions");
	    		}
    		}
	        
		}
	}
	
	String generateTSV (Glycoprotein gp) {
		StringBuffer input = new StringBuffer();
		// generate TSV 
		for (Site site: gp.getSites()) {
			for (GlycanInSite g: site.getGlycans()) {
				String line = gp.getUniprotId() + "\t";
				if (g.getGlycan() == null) continue;
				switch (site.getType()) {
				case ALTERNATIVE:
					//TODO determine what to do, for now discard the whole glycoprotein
					return null;
					/*for (Position pos: site.getPosition().getPositionList()) {
						line += pos.getLocation() + "\t" + g.getGlycan().getGlytoucanID();
						input.append(line + "\n");
						line = gp.getUniprotId() + "\t";
					}
					break;*/
				case EXPLICIT:
					line += site.getPosition().getPositionList().get(0).getLocation() + "\t";
					line += g.getGlycan().getGlytoucanID();
					input.append(line + "\n");
					break;
				case RANGE:
					//TODO determine what to do, for now discard the whole glycoprotein
					return null;
					//line += site.getPosition().getPositionList().get(0).getLocation() + "\t";
					//line += g.getGlycan().getGlytoucanID();
					//input.append(line + "\n");
					//break;
				case UNKNOWN:
					line += "?\t";
					line += g.getGlycan().getGlytoucanID();
					input.append(line + "\n");
					break;
				default:
					continue;
				}
				
				
			}
		}
		return input.toString();
	}
	
	public static void main(String[] args) {
		
		Glycoprotein gp = new Glycoprotein();
		gp.setUniprotId("P55083");
		Site site = new Site();
		site.setType(GlycoproteinSiteType.EXPLICIT);
		site.setGlycans(new ArrayList<>());
		GlycanInSite gis = new GlycanInSite();
		gis.setGlycan(new Glycan());
		gis.getGlycan().setGlytoucanID("G42124LM");
		site.setPosition(new SitePosition());
		site.getPosition().setPositionList(new ArrayList<>());
		Position pos = new Position();
		pos.setLocation(137L);
		site.getPosition().getPositionList().add(pos);
		site.getGlycans().add(gis);
		gp.setSites(new ArrayList<>());
		gp.getSites().add(site);
		
		try {
			GlyCombUtil util = GlyCombUtil.getInstance();
			util.setApiKey("eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhdWQiOiI4NDFhMmVmMC0zMWU1LTM0OWEtOWViZC1hNTQ2NjMxZjNkZTAiLCJpc3MiOiJnbHljb21iIiwiZXhwIjoxNzY3OTEyNTc3fQ.FxClVKscxqQBiTTH0-0jRxPhcSCzTFglq2lv_Mf0Y0w");
			//util.requestRegistration(gp);
			gp.setGlycombReceiptNumber("b1f8690d-ce09-4a83-a376-3738c377a66d");
			util.publish(gp);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
