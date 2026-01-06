package org.glygen.tablemaker.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

public class GlyCombUtil {
	
	String apiKey;
	
	static GlyCombUtil instance;
	
	static final String validationURL = "https://glycomb.glycosmos.org/api/input-validation";
	static final String registrationRequestURL = "https://glycomb.glycosmos.org/auth/api/registration-request";
	static final String publishURL = "https://glycomb.glycosmos.org/auth/api/publish-entry/";
	
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
	
	@SuppressWarnings("serial")
    static HttpHeaders createHeaders(String apiKey, MediaType contentType){
	   return new HttpHeaders() {{
	         byte[] encodedAuth = Base64.getEncoder().encode( 
	            apiKey.getBytes(Charset.forName("US-ASCII")) );
	         String authHeader = "Bearer " + new String( encodedAuth );
	         set( "Authorization", authHeader );
	         setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
	         setContentType(contentType);
	      }};
	}
	
	
	public void requestRegistration (Glycoprotein gp) throws IOException {
		String input = generateTSV(gp);
		
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
            
            // check for errors
            
            
            // step 2 - registration request
            // get raw_data from response and use in registration request
            
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
					for (Position pos: site.getPosition().getPositionList()) {
						line += pos.getLocation() + "\t" + g.getGlycan().getGlytoucanID();
						input.append(line + "\n");
						line = gp.getUniprotId() + "\t";
					}
					break;
				case EXPLICIT:
					line += site.getPosition().getPositionList().get(0).getLocation() + "\t";
					line += g.getGlycan().getGlytoucanID();
					input.append(line + "\n");
					break;
				case RANGE:
					line += site.getPosition().getPositionList().get(0).getLocation() + "\t";
					line += g.getGlycan().getGlytoucanID();
					input.append(line + "\n");
					break;
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
			GlyCombUtil.getInstance().requestRegistration(gp);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
