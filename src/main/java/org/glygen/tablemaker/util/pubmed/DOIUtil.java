package org.glygen.tablemaker.util.pubmed;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

public class DOIUtil {
	
	String doiUrl = "https://doi.org/";
	
	String pubmedIdUrl = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&format=json&term=";
		
	
	public DTOPublication getPublication (String doi) throws IOException, Exception {
		String pubUrl = doiUrl + doi;
		RestTemplate restTemplate = new RestTemplate();

        // Create headers
        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", "application/vnd.citationstyles.csl+json");

        // Create an HTTP entity with headers
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
	        // Make the GET request
	        ResponseEntity<String> response = restTemplate.exchange(
	                pubUrl, 
	                HttpMethod.GET, 
	                entity, 
	                String.class);
	        String json = response.getBody();
	        return createFromJson (json);
        } catch (HttpStatusCodeException exception) {
            throw new IOException ("Failed to retrieve the publication with DOID. Status code: " 
            		+ exception.getStatusCode() + ". Response: " + exception.getResponseBodyAsString());
        }
		 
		/*String result = PubmedUtil.makeHttpRequest(new URL(this.pubmedIdUrl + doi));
		Integer pubmedId = getPubmedId(result);
		if (pubmedId != null) {
			return new PubmedUtil().createFromPubmedId(pubmedId);
		}
		return null;*/
	}
	
	private DTOPublication createFromJson(String json) {
		DTOPublication pub = new DTOPublication();
		final JSONObject obj = new JSONObject(json);
		if (obj.has("page")) {
			String pageRange = obj.getString("page");
			if (pageRange != null) {
				String[] parts = pageRange.split("-");
				if (parts.length > 1) {
					pub.setStartPage(parts[0].trim());
					pub.setEndPage(parts[1].trim());
				}
			}
		}
		if (obj.has("DOI")) pub.setDoiId(obj.getString("DOI"));
		if (obj.has("title")) pub.setTitle(obj.getString("title"));
		if (obj.has("volume")) pub.setVolume(obj.getString("volume"));
		if (obj.has("container-title")) pub.setJournal(obj.getString("container-title"));
		if (obj.has("type")) pub.setType(obj.getString("type"));
		if (obj.has("created")) {
			JSONObject dateObject = obj.getJSONObject("created");
			if (dateObject != null) {
				String dateTime = dateObject.getString("date-time");
				if (dateTime != null && dateTime.length() > 4) {
					try {
						pub.setYear(Integer.parseInt (dateTime.substring(0, 4)));
					} catch (NumberFormatException e) {
						//ignore
					}
				}
			}
		}
		if (obj.has("author")) {
			JSONArray authors = obj.getJSONArray("author");
			List<DTOPublicationAuthor> aList = new ArrayList<>();
			for (int i=0; i < authors.length(); i++) {
				JSONObject author = authors.getJSONObject(i);
				DTOPublicationAuthor a = new DTOPublicationAuthor();
				a.setFirstName(author.getString("given"));
				a.setLastName(author.getString("family"));
				a.setOrder(i);
				aList.add(a);
			}
			pub.setAuthors(aList);
		}
		return pub;
	}

	Integer getPubmedId (String resultJson) {
		final JSONObject obj = new JSONObject(resultJson);
		final JSONObject searchResult = obj.getJSONObject("esearchresult");
		final JSONArray ids = searchResult.getJSONArray("idlist");
		if (ids.length() > 0) return ids.getInt(0);
		return null;
	}
	
	public static void main(String[] args) {
		DTOPublication pub;
		try {
			pub = new DOIUtil().getPublication("10.3390/cimb45110575");
			System.out.println (pub.getFormattedAuthor());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
