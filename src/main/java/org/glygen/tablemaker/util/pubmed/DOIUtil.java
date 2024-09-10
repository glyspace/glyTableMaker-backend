package org.glygen.tablemaker.util.pubmed;

import java.io.IOException;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONObject;

public class DOIUtil {
	
	String apiUrl = "https://api.test.datacite.org/dois/";
	
	String pubmedIdUrl = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&format=json&term=";
		
	
	public DTOPublication getPublication (String doi) throws IOException, Exception {
		String result = PubmedUtil.makeHttpRequest(new URL(this.pubmedIdUrl + doi));
		Integer pubmedId = getPubmedId(result);
		if (pubmedId != null) {
			return new PubmedUtil().createFromPubmedId(pubmedId);
		}
		return null;
	}
	
	Integer getPubmedId (String resultJson) {
		final JSONObject obj = new JSONObject(resultJson);
		final JSONObject searchResult = obj.getJSONObject("esearchresult");
		final JSONArray ids = searchResult.getJSONArray("idlist");
		if (ids.length() > 0) return ids.getInt(0);
		return null;
	}

}
