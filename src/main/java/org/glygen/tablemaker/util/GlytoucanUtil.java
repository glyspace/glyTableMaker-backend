package org.glygen.tablemaker.util;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.eurocarbdb.MolecularFramework.io.SugarImporterException;
import org.eurocarbdb.MolecularFramework.io.GlycoCT.SugarExporterGlycoCTCondensed;
import org.eurocarbdb.MolecularFramework.io.GlycoCT.SugarImporterGlycoCTCondensed;
import org.eurocarbdb.MolecularFramework.sugar.Sugar;
import org.eurocarbdb.MolecularFramework.util.visitor.GlycoVisitorException;
import org.glycoinfo.GlycanFormatconverter.Glycan.GlyContainer;
import org.glycoinfo.GlycanFormatconverter.io.GlycoCT.GlyContainerToSugar;
import org.glycoinfo.GlycanFormatconverter.util.exchange.WURCSGraphToGlyContainer.WURCSGraphToGlyContainer;
import org.glycoinfo.WURCSFramework.io.GlycoCT.WURCSExporterGlycoCT;
import org.glycoinfo.WURCSFramework.util.WURCSException;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;
import org.glycoinfo.WURCSFramework.util.validation.WURCSValidator;
import org.glycoinfo.WURCSFramework.wurcs.graph.WURCSGraph;
import org.glygen.tablemaker.exception.GlytoucanAPIFailedException;
import org.glygen.tablemaker.exception.GlytoucanFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;

public class GlytoucanUtil {
	public static String glytoucanLinkURL = "https://glytoucan.org/Structures/Glycans/";
    static String glycanURL   = "https://glytoucan.org/api/entry/";
    static String registerURL = "https://glytoucan.org/api/submission";
    static String searchURL   = "https://glytoucan.org/api/entry/search";
    static String statusURL   = "https://glytoucan.org/api/submission/wurcs/";
 
    private static final Logger logger = LoggerFactory.getLogger(GlytoucanUtil.class);
 
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);
    private static final int MAX_LOGGED_BODY = 3000;
 
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();
    private static final GlytoucanUtil INSTANCE = new GlytoucanUtil();
 
    private volatile String apiKey;
    private volatile String userId;
 
    private GlytoucanUtil() {
    }
 
    public static GlytoucanUtil getInstance() {
        return INSTANCE;
    }
 
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
 
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
	/**
	 * checks the status of registered glycan by the hashkey that was assigned during registration, returns glytoucan id if assigned
	 * 
	 * @param hashKey hash key to check for status
	 * @return accession number if assigned
	 * @throws GlytoucanFailedException throws GlytoucanFailedException if there were errors during registration
	 */
    public String checkBatchStatus(String hashKey) throws GlytoucanFailedException {
        if (isBlank(hashKey)) return null;
 
        ApiResponse r;
        try {
            r = get(statusURL + hashKey.trim());
        } catch (IOException e) {
            // Transient problem: treat as "not known yet" so the caller can retry later
            logger.error("Cannot reach GlyTouCan to check submission " + hashKey + ": " + e.getMessage(), e);
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while checking submission " + hashKey);
            return null;
        }
 
        JsonNode contents = r.contents();
        if (contents.isObject()) {
            String accession = text(contents, "accession");
            if (accession != null) return accession;
 
            String validationError = text(contents, "validation_error");
            if (validationError != null) {
                logger.info("Submission " + hashKey + " failed validation: " + validationError);
                throw new GlytoucanFailedException("Registration error", validationError);
            }
        }
 
        String error = findError(r.json);
        if (error != null) {
            logger.warn("GlyTouCan returned an error checking submission " + hashKey
                    + " (HTTP " + r.status + "): " + error);
        } else if (!r.isSuccess() || r.json == null) {
            logger.warn("Unexpected response checking submission " + hashKey + " " + r.describe());
        }
        // No accession yet
        return null;
    }
	
    /**
     * Registers the glycan with GlyTouCan.
     *
     * @param wurcsSequence WURCS sequence to register
     * @return the submission number (hash key) to use with {@link #checkBatchStatus(String)}
     * @throws GlytoucanFailedException if GlyTouCan rejects the glycan
     * @throws GlytoucanAPIFailedException if GlyTouCan cannot be reached or the response format is unexpected
     */
    public String registerGlycan(String wurcsSequence) throws GlytoucanFailedException, GlytoucanAPIFailedException {
        if (isBlank(wurcsSequence)) return null;
 
        ApiResponse r;
        try {
            r = post(registerURL, new Sequence(wurcsSequence));
        } catch (IOException e) {
            logger.error("Cannot reach GlyTouCan to register glycan: " + e.getMessage() + ". Sequence: " + wurcsSequence, e);
            throw new GlytoucanAPIFailedException("GlyTouCan registration API is not reachable", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GlytoucanAPIFailedException("GlyTouCan registration was interrupted", e);
        }
 
        JsonNode contents = r.contents();
        if (contents.isObject()) {
            String submissionNumber = text(contents, "submission_number");
            if (submissionNumber != null) return submissionNumber;
        }
 
        String error = findError(r.json);
        if (error != null) {
            if (r.isServerError()) {
                logger.error("GlyTouCan server error during registration " + r.describe());
                throw new GlytoucanAPIFailedException("GlyTouCan registration API failed: " + error,
                        new IOException(r.describe()));
            }
            logger.info("GlyTouCan rejected glycan: " + error + ". Sequence: " + wurcsSequence);
            throw new GlytoucanFailedException("Cannot register the glycan. Reason: " + error, r.body);
        }
 
        logger.error("GlyTouCan registration API has changed. No submission number in the response " + r.describe());
        throw new GlytoucanAPIFailedException("GlyTouCan registration API has been modified!",
                new IOException("Cannot find submission number in the response " + r.describe()));
    }
	
	/**
     * Looks up the GlyTouCan accession number for a WURCS sequence.
     *
     * @param wurcsSequence WURCS sequence to search
     * @return accession number, or null if the glycan is not found or GlyTouCan reports an error for it
     * @throws GlytoucanAPIFailedException if GlyTouCan cannot be reached or the response format is unexpected
     */
    public String getAccessionNumber(String wurcsSequence) throws GlytoucanAPIFailedException {
        if (isBlank(wurcsSequence)) return null;
 
        ApiResponse r;
        try {
            r = post(searchURL, new Sequence(wurcsSequence));
        } catch (IOException e) {
            logger.error("Cannot reach GlyTouCan search API: " + e.getMessage(), e);
            throw new GlytoucanAPIFailedException("GlyTouCan retrieval API is not working", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GlytoucanAPIFailedException("GlyTouCan search was interrupted", e);
        }
 
        JsonNode contents = r.contents();
        if (contents.isObject()) {
            String accession = text(contents, "accession");
            if (accession != null) return accession;
        }
 
        String error = findError(r.json);
        if (error != null) {
            if (r.isServerError()) {
                logger.error("GlyTouCan server error during search " + r.describe());
                throw new GlytoucanAPIFailedException("GlyTouCan retrieval API failed: " + error,
                        new IOException(r.describe()));
            }
            logger.info("Cannot get accession number of the glycan. Reason: " + error);
            return null;
        }
 
        logger.error("GlyTouCan search API has changed. No accession in the response " + r.describe());
        throw new GlytoucanAPIFailedException("GlyTouCan search API has been modified!",
                new IOException("Cannot find accession in the response " + r.describe()));
    }
	
	/**
	 * calls Glytoucan API to retrieve the glycan with the given accession number
	 * 
	 * @param accessionNumber the glytoucan id to search
	 * @return WURCS sequence if the glycan is found, null otherwise
	 */
	public String retrieveGlycan(String accessionNumber) {
        if (isBlank(accessionNumber)) return null;
        String acc = accessionNumber.trim();
 
        ApiResponse r;
        try {
            r = get(glycanURL + acc);
        } catch (IOException e) {
            logger.warn("Cannot reach GlyTouCan to retrieve " + acc + ": " + e.getMessage(), e);
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while retrieving " + acc);
            return null;
        }
 
        JsonNode contents = r.contents();
        if (contents.isObject()) {
            String wurcs = text(contents, "validated_wurcstext");
            if (wurcs != null) return wurcs;
 
            if (contents.path("archive_flag").asBoolean(false)) {
                logger.warn(acc + " has been archived");
                return null;
            }
        }
 
        String error = findError(r.json);
        if (error != null) {
            logger.warn("GlyTouCan returned an error retrieving " + acc + " (HTTP " + r.status + "): " + error);
        } else {
            logger.warn("Unexpected response retrieving " + acc + " " + r.describe());
        }
        return null;
    }
	
	private ApiResponse get(String url) throws IOException, InterruptedException {
        return execute(HttpRequest.newBuilder().uri(URI.create(url)).GET());
    }
 
    private ApiResponse post(String url, Object body) throws IOException, InterruptedException {
        String requestBody = MAPPER.writeValueAsString(body);   // JsonProcessingException is an IOException
        return execute(HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8)));
    }
	
	private ApiResponse execute(HttpRequest.Builder builder) throws IOException, InterruptedException {
        createHeaders(userId, apiKey).forEach(builder::header);
        HttpRequest request = builder.timeout(REQUEST_TIMEOUT).build();
 
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String body = response.body();
 
        JsonNode json = null;
        if (body != null && !body.isBlank()) {
            try {
                JsonNode node = MAPPER.readTree(body);
                if (node != null && node.isObject()) json = node;
            } catch (JsonProcessingException e) {
                // Not JSON (e.g. an HTML error page). Leave json null; callers log the raw body.
                logger.debug("Response from " + request.uri() + " is not JSON: " + e.getOriginalMessage());
            }
        }
        return new ApiResponse(request.uri().toString(), response.statusCode(), body, json);
    }

	Map<String, String> createHeaders(String userId, String apiKey) {
	    Map<String, String> headers = new HashMap<>();
	    String auth = userId + ":" + apiKey;
        byte[] encodedAuth = Base64.getEncoder().encode( 
           auth.getBytes(Charset.forName("US-ASCII")) );
        String authHeader = "Basic " + new String( encodedAuth );
	    headers.put("Authorization", authHeader);
	    headers.put("Content-Type", "application/json");
	    return headers;
	}
	
	/**
     * Looks for an error in the places GlyTouCan is known to put one:
     * top-level "errcode", contents.error / contents.message, or a top-level error/message.
     *
     * @return the error text, or null if none was found
     */
    private static String findError(JsonNode root) {
        if (root == null) return null;
 
        String errcode = text(root, "errcode");
        if (errcode != null) {
            String msg = text(root, "message");
            return msg != null ? errcode + ": " + msg : errcode;
        }
 
        JsonNode contents = root.path("contents");
        if (contents.isObject()) {
            String err = firstNonNull(text(contents, "error"), text(contents, "message"));
            if (err != null) return err;
        }
 
        return firstNonNull(text(root, "message"), text(root, "error"));
    }
	
	private static String text(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode v = node.path(field);
        if (v.isMissingNode() || v.isNull()) return null;
        String s = (v.isValueNode() ? v.asText() : v.toString()).trim();
        return s.isEmpty() ? null : s;
    }
 
    private static String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }
 
    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
	
	private static final class ApiResponse {
        final String url;
        final int status;
        final String body;
        final JsonNode json;
 
        ApiResponse(String url, int status, String body, JsonNode json) {
            this.url = url;
            this.status = status;
            this.body = body;
            this.json = json;
        }
 
        boolean isSuccess() {
            return status >= 200 && status < 300;
        }
 
        boolean isServerError() {
            return status >= 500;
        }
 
        JsonNode contents() {
            return json == null ? MissingNode.getInstance() : json.path("contents");
        }
 
        String describe() {
            String b = body == null ? "<empty>" : body;
            if (b.length() > MAX_LOGGED_BODY) b = b.substring(0, MAX_LOGGED_BODY) + "...(truncated)";
            return "[" + url + ", HTTP " + status + "]: " + b;
        }
    }

	
	public static Sugar getSugarFromWURCS (String wurcsSequence) throws IOException {
	    try {
            WURCSFactory wf = new WURCSFactory(wurcsSequence);
            WURCSGraph graph = wf.getGraph();

            // Exchange WURCSGraph to GlyContainer
            WURCSGraphToGlyContainer wg2gc = new WURCSGraphToGlyContainer();
            wg2gc.start(graph);
            GlyContainer t_gc = wg2gc.getGlycan();

            // Exchange GlyConatainer to Sugar
            GlyContainerToSugar t_export = new GlyContainerToSugar();
            t_export.start(t_gc);
            Sugar t_sugar = t_export.getConvertedSugar();
            return t_sugar;
	    } catch (Exception e) {
	        throw new IOException ("Cannot be converted to Sugar object. Reason: " + e.getMessage());
	    }
	}
	
	
	public static void main(String[] args) {
		
		GlytoucanUtil util = GlytoucanUtil.getInstance();
        util.setUserId(System.getenv("GLYTOUCAN_USER_ID"));
        util.setApiKey(System.getenv("GLYTOUCAN_API_KEY"));
        
		try {
			String glyTouCanId = GlytoucanUtil.getInstance().registerGlycan("WURCS=2.0/3,3,2/[a1122h-1b_1-5][a1122h-1a_1-5_6*OPOC/3O/3=O][a1122h-1a_1-5_?*OPOC/3O/3=O]/1-2-3/a3-b1_a6-c1");
			System.out.println(glyTouCanId);
		} catch (Exception e1) {
			System.out.println ("Registration Error " + e1.getMessage());
		}
		
		try {
			GlytoucanUtil.getInstance().checkBatchStatus("d11c7ed7c2990f89f2746934fd800c026106061d8a8d0f9da30f190f6bc66f0a");
		} catch (GlytoucanFailedException e) {
			System.out.println ("Received error: " + e.getErrorJson());
		}
		
		String glycoCTSeq = "RES\n"
		        + "1b:b-dglc-HEX-1:5\n"
		        + "2s:n-acetyl\n"
		        + "3b:b-dglc-HEX-1:5\n"
		        + "4s:n-acetyl\n"
		        + "5b:b-dman-HEX-1:5\n"
		        + "6b:a-dman-HEX-1:5\n"
		        + "7b:b-dglc-HEX-1:5\n"
		        + "8s:n-acetyl\n"
		        + "9b:b-dgal-HEX-1:5\n"
		        + "10b:a-dgro-dgal-NON-2:6|1:a|2:keto|3:d\n"
		        + "11s:n-acetyl\n"
		        + "12b:a-dman-HEX-1:5\n"
		        + "13b:b-dglc-HEX-1:5\n"
		        + "14s:n-acetyl\n"
		        + "15b:b-dgal-HEX-1:5\n"
		        + "16b:a-dgro-dgal-NON-2:6|1:a|2:keto|3:d\n"
		        + "17s:n-acetyl\n"
		        + "18b:a-lgal-HEX-1:5|6:d\n"
		        + "LIN\n"
		        + "1:1d(2+1)2n\n"
		        + "2:1o(4+1)3d\n"
		        + "3:3d(2+1)4n\n"
		        + "4:3o(4+1)5d\n"
		        + "5:5o(3+1)6d\n"
		        + "6:6o(2+1)7d\n"
		        + "7:7d(2+1)8n\n"
		        + "8:7o(4+1)9d\n"
		        + "9:9o(3+2)10d\n"
		        + "10:10d(5+1)11n\n"
		        + "11:5o(6+1)12d\n"
		        + "12:12o(2+1)13d\n"
		        + "13:13d(2+1)14n\n"
		        + "14:13o(4+1)15d\n"
		        + "15:15o(3+2)16d\n"
		        + "16:16d(5+1)17n\n"
		        + "17:1o(6+1)18d";
        WURCSExporterGlycoCT exporter = new WURCSExporterGlycoCT();
        try {
            exporter.start(glycoCTSeq);
        } catch (SugarImporterException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (GlycoVisitorException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (WURCSException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        String wurcs = exporter.getWURCS(); 
        System.out.println(wurcs);
        
        wurcs = "WURCS=2.0/2,2,1/[h2122h_6*OSO/3=O/3=O][a2112h-1b_1-5]/1-2/a4-b1";
        System.out.println(wurcs);
        try {
	        String glyTouCanId = GlytoucanUtil.getInstance().getAccessionNumber(wurcs);
	        System.out.println(glyTouCanId);
	        
	        wurcs = "WURCS=2.0/3,4,4/[a2112h-1a_1-5][a2122h-1b_1-5_2*NCC/3=O][a2211m-1b_1-5]/1-2-2-3/a6-b1_b4-d1_c1-b3%.6%_a1-d3~n";
	        glyTouCanId = GlytoucanUtil.getInstance().getAccessionNumber(wurcs);
	        System.out.println(glyTouCanId);
        } catch (Exception e) {
        	e.printStackTrace();
        }
        try {
            SugarImporterGlycoCTCondensed importer = new SugarImporterGlycoCTCondensed();
            Sugar sugar = importer.parse(glycoCTSeq);
            SugarExporterGlycoCTCondensed exporter2 = new SugarExporterGlycoCTCondensed();
            exporter2.start(sugar);
            System.out.println(exporter2.getHashCode());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        /*wurcs = "WURCS=2.0/5,10,9/[a2122h-1b_1-5_2*NCC/3=O][a1122h-1b_1-5][a1122h-1a_1-5][][a1221m-1a_1-5]/1-1-2-3-4-4-3-4-4-5/a4-b1_a6-j1_b4-c1_c3-d1_c6-g1_d2-e0_d4-f0_g2-h0_g6-i0";
        String errors = GlytoucanUtil.getInstance().validateGlycan(wurcs);
*/
        
        wurcs = "WURCS=2.0/6,11,10/[a2122h-1x_1-?_2*NCC/3=O][a2122h-1b_1-5_2*NCC/3=O][a1122h-1b_1-5][a1122h-1a_1-5][a2112h-1b_1-5][Aad21122h-2a_2-6_5*N]/1-2-3-4-2-5-6-4-2-5-6/a4-b1_b4-c1_c3-d1_c6-h1_d2-e1_e4-f1_f6-g2_h2-i1_i4-j1_j6-k2";
        try {
			GlytoucanUtil.getSugarFromWURCS(wurcs);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        WURCSValidator validator = new WURCSValidator();
        validator.start(wurcs);
        if (validator.getReport().hasError()) {
            String errorMessage = "";
            for (String error: validator.getReport().getErrors()) {
                errorMessage += error + ", ";
            }
            errorMessage = errorMessage.substring(0, errorMessage.lastIndexOf(","));
            System.out.println (errorMessage);
        } 
    }
}

class Sequence {
	String sequence;
	
	public Sequence() {
    }
 
    public Sequence(String sequence) {
        this.sequence = sequence;
    }
	
	public void setSequence (String s) {
		this.sequence = s;
	}
	
	public String getSequence() {
		return sequence;
	}
}