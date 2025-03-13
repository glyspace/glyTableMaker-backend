package org.glygen.tablemaker.util;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
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
import org.glycoinfo.WURCSFramework.wurcs.graph.WURCSGraph;
import org.glygen.tablemaker.exception.GlytoucanFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GlytoucanUtil {
	
	String apiKey;
	String userId;
	
	static String glycanURL = "https://sparqlist.glycosmos.org/sparqlist/api/gtc_wurcs_by_accession?accNum=";
	static String retrieveURL ="api/WURCS2GlyTouCan";
	static String alternativeRetrieveURL = "sparqlist/wurcs2gtcids";
	static String registerURL = "https://api.glytoucan.org/glycan/register";
	static String validateURL = "wurcsframework/wurcsvalidator/1.0.1/";
	static String apiURL = "https://api.glycosmos.org/";
	static String retrieveAPIURL ="https://sparqlist.glyconavi.org/";
	static String statusURL = "https://sparqlist.glycosmos.org/sparqlist/api/check_batch_processing_by_hashkey?hashKey=";
	
	
	//https://api.glycosmos.org/sparqlist/wurcs2gtcids?wurcs=
	
	private static RestTemplate restTemplate = new RestTemplate();
	
	final static Logger logger = LoggerFactory.getLogger(GlytoucanUtil.class);
	
	static GlytoucanUtil instance;
	
	private GlytoucanUtil() {
	}
	
	public static GlytoucanUtil getInstance () {
		if (instance == null)
			instance = new GlytoucanUtil();
		return instance;
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
	public String checkBatchStatus (String hashKey) throws GlytoucanFailedException {
		
		HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(createHeaders(userId, apiKey));
		try {
			ResponseEntity<StatusResponse[]> response = restTemplate.exchange(statusURL+hashKey, HttpMethod.GET, requestEntity, StatusResponse[].class);
			for (StatusResponse status: response.getBody()) {
				if (status.batch_value.equalsIgnoreCase("invalid")) {
					String errorJson = null;
					try {
						errorJson = new ObjectMapper().writeValueAsString(response.getBody());
					} catch (JsonProcessingException e) {
						logger.warn ("Could not write error response as json ", e);
					}
					throw new GlytoucanFailedException(status.batch_p + " " + status.batch_value, errorJson);
				} else if (status.batch_p.contains("AccessionNumber")) {  // there is an accession number
					return status.batch_value.substring(status.batch_value.lastIndexOf("/")+1); 
				}
			}
		} catch (HttpClientErrorException e) {
			logger.info("Exception retrieving glycan " + ((HttpClientErrorException) e).getResponseBodyAsString());
		} catch (HttpServerErrorException e) {
			logger.info("Exception retrieving glycan " + ((HttpServerErrorException) e).getResponseBodyAsString());
		}
		return null;
	}
	
	public String registerGlycan (String wurcsSequence) throws GlytoucanFailedException {
	    
	    Sequence input = new Sequence();
	    input.setSequence(wurcsSequence);
	    
	    HttpEntity<Sequence> requestEntity = new HttpEntity<Sequence>(input, createHeaders(userId, apiKey));
		
		try {
			ResponseEntity<Response> response = restTemplate.exchange(registerURL, HttpMethod.POST, requestEntity, Response.class);
			return response.getBody().getMessage();
		} catch (HttpClientErrorException e) {
			logger.error("Client Error: Exception adding glycan " + ((HttpClientErrorException) e).getResponseBodyAsString());
			String error = ((HttpClientErrorException) e).getResponseBodyAsString();
			try {
				RegistrationErrorMessage m = new ObjectMapper().readValue(error, RegistrationErrorMessage.class);
				throw new GlytoucanFailedException ("Cannot register the glycan. Reason: " + m.error + " . Message: " + m.message, error);
			} catch (JsonProcessingException e1) {
				logger.error("Could not parse the error message: ", e1);
			}
			logger.info("Sequence: " + wurcsSequence);
		} catch (HttpServerErrorException e) {
			logger.error("Server Error: Exception adding glycan " + ((HttpServerErrorException) e).getResponseBodyAsString());
			String error = ((HttpServerErrorException) e).getResponseBodyAsString();
			try {
				RegistrationErrorMessage m = new ObjectMapper().readValue(error, RegistrationErrorMessage.class);
				throw new GlytoucanFailedException ("Cannot register the glycan. Reason: " + m.error + " . Message: " + m.message, error);
			} catch (JsonProcessingException e1) {
				logger.error("Could not parse the error message: ", e1);
			}
			logger.info("Sequence: " + wurcsSequence);
		} catch (Exception e) {
		    logger.error("General Error: Exception adding glycan " + e.getMessage());
            logger.info("Sequence: " + wurcsSequence);
		}
		
		return null;
	}
	
	public String getAccessionNumber (String wurcsSequence) {
		String accessionNumber = null;
        
		//HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(createHeaders(userId, apiKey));
		
		String url = UriComponentsBuilder.fromHttpUrl(apiURL + alternativeRetrieveURL)
                .queryParam("wurcs", wurcsSequence)
                .toUriString();

		//String url = apiURL + alternativeRetrieveURL + "?wurcs=" + wurcsSequence;
		/*try {
			//ResponseEntity<GlytoucanResponse[]> response = restTemplate.exchange(uriComponents.toUri(), HttpMethod.GET, requestEntity, GlytoucanResponse[].class);
			ResponseEntity<GlytoucanResponse[]>  response = restTemplate.getForEntity(url, GlytoucanResponse[].class);
			
			if (response.getBody().length == 0) {
			    logger.info ("No accession number is found! " + wurcsSequence);
			    return null;
			}
			if (response.getBody()[0].message  != null) {
			    logger.info("Error retrieving glycan " + response.getBody()[0].message);
			}
			return response.getBody()[0].id;
		} catch (HttpClientErrorException e) {
			logger.info("Exception retrieving glycan " + ((HttpClientErrorException) e).getResponseBodyAsString());
		} catch (HttpServerErrorException e) {
			logger.info("Exception retrieving glycan " + ((HttpServerErrorException) e).getResponseBodyAsString());
		}
		*/
		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            logger.debug("Executing request: " + request.getRequestLine());
            HttpResponse response = httpClient.execute(request);
            if (response.getStatusLine().getStatusCode() != 200) {
            	logger.error("Glytoucan retrieval API is not working. Status code: " + response.getStatusLine());
            	return null;
            }
            String responseBody = EntityUtils.toString(response.getEntity());
            ObjectMapper objectMapper = new ObjectMapper();
            GlytoucanResponse[] resp = objectMapper.readValue(responseBody, GlytoucanResponse[].class);
            if (resp.length == 0) {
			    logger.info ("No accession number is found! " + wurcsSequence);
			    return null;
			}
			if (resp[0].message  != null) {
			    logger.info("Error retrieving glycan " + resp[0].message);
			}
			return resp[0].id;
        } catch (Exception e) {
        	logger.info("Exception retrieving glycan " + ((HttpServerErrorException) e).getResponseBodyAsString());
        }
		
		return accessionNumber;
	}
	
	/**
	 * use Glycosmos validation API to validate a glycan
	 * 
	 * @param wurcsSequence the glycan sequence in WURCS format
	 * @return error string if there is a validation error or an error during validation, null if there are no errors
	 */
	public String validateGlycan (String wurcsSequence) {
	    try {
            Map<String, String> uriVariables = new HashMap<>();
            uriVariables.put("wurcs", wurcsSequence);
            
            UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(apiURL)
                    .path(validateURL)
                    .pathSegment("{wurcs}")
                    .buildAndExpand(uriVariables)
                    .encode();
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(createHeaders(userId, apiKey));
            ResponseEntity<ValidationResponse> response = restTemplate.exchange(uriComponents.toUri(), HttpMethod.GET, requestEntity, ValidationResponse.class);
            if (response.getBody().getM_mapTypeToReports().getErrors() != null && !response.getBody().getM_mapTypeToReports().getErrors().isEmpty()) {
                return response.getBody().getM_mapTypeToReports().getErrors().get(0).toString();
            }
            return null;
        } catch (HttpClientErrorException e) {
            logger.info("Exception validating glycan " + ((HttpClientErrorException) e).getResponseBodyAsString());
            return "Exception validating glycan " + ((HttpClientErrorException) e).getResponseBodyAsString();
        } catch (HttpServerErrorException e) {
            logger.info("Exception validating glycan " + ((HttpServerErrorException) e).getResponseBodyAsString());
            return "Exception validating glycan " + ((HttpServerErrorException) e).getResponseBodyAsString();
        } 
	}
	
	/**
	 * calls Glytoucan API to retrieve the glycan with the given accession number
	 * 
	 * @param accessionNumber the glytoucan id to search
	 * @return WURCS sequence if the glycan is found, null otherwise
	 */
	public String retrieveGlycan (String accessionNumber) {
		String sequence = null;
		
		String url = glycanURL + accessionNumber;
		HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(createHeaders(userId, apiKey));
		try {
			ResponseEntity<RetrieveResponse[]> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, RetrieveResponse[].class);
			RetrieveResponse[] arr = response.getBody();
			if (arr.length > 0)
			    return response.getBody()[0].getWurcsLabel();
			else 
			    return null;
		} catch (HttpClientErrorException e) {
			logger.info("Exception retrieving glycan " + ((HttpClientErrorException) e).getResponseBodyAsString());
		} catch (HttpServerErrorException e) {
			logger.info("Exception adding glycan " + ((HttpServerErrorException) e).getResponseBodyAsString());
		}
		
		return sequence;
	}
	
	@SuppressWarnings("serial")
    static HttpHeaders createHeaders(String username, String password){
	   return new HttpHeaders() {{
	         String auth = username + ":" + password;
	         byte[] encodedAuth = Base64.getEncoder().encode( 
	            auth.getBytes(Charset.forName("US-ASCII")) );
	         String authHeader = "Basic " + new String( encodedAuth );
	         set( "Authorization", authHeader );
	         setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
	         setContentType(MediaType.APPLICATION_JSON);
	      }};
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
		
		GlytoucanUtil.getInstance().setApiKey("6d9fbfb1c0a52cbbffae7c113395a203ae0e3995a455c42ff3932862cbf7e62a");
        GlytoucanUtil.getInstance().setUserId("ff2dda587eb4597ab1dfb995b520e99b7ef68d7786af0f3ea626555e2c609c3d");
		
		String glyTouCanId;
		try {
			glyTouCanId = GlytoucanUtil.getInstance().registerGlycan(null);
			System.out.println(glyTouCanId);
		} catch (Exception e1) {
			System.out.println ("Registration Error " + e1.getMessage());
		}
		
		try {
			GlytoucanUtil.getInstance().checkBatchStatus("6407df5cefbbd62860b9f762158657f1663d51423e25d85e1293a903c0681031");
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
        glyTouCanId = GlytoucanUtil.getInstance().getAccessionNumber(wurcs);
        System.out.println(glyTouCanId);
        
        wurcs = "WURCS=2.0/3,4,4/[a2112h-1a_1-5][a2122h-1b_1-5_2*NCC/3=O][a2211m-1b_1-5]/1-2-2-3/a6-b1_b4-d1_c1-b3%.6%_a1-d3~n";
        glyTouCanId = GlytoucanUtil.getInstance().getAccessionNumber(wurcs);
        System.out.println(glyTouCanId);
        try {
            SugarImporterGlycoCTCondensed importer = new SugarImporterGlycoCTCondensed();
            Sugar sugar = importer.parse(glycoCTSeq);
            SugarExporterGlycoCTCondensed exporter2 = new SugarExporterGlycoCTCondensed();
            exporter2.start(sugar);
            System.out.println(exporter2.getHashCode());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        wurcs = "WURCS=2.0/5,10,9/[a2122h-1b_1-5_2*NCC/3=O][a1122h-1b_1-5][a1122h-1a_1-5][][a1221m-1a_1-5]/1-1-2-3-4-4-3-4-4-5/a4-b1_a6-j1_b4-c1_c3-d1_c6-g1_d2-e0_d4-f0_g2-h0_g6-i0";
        String errors = GlytoucanUtil.getInstance().validateGlycan(wurcs);
        System.out.println("Validation result" + errors);
    }
}

class Sequence {
	String sequence;
	
	public void setSequence (String s) {
		this.sequence = s;
	}
	
	public String getSequence() {
		return sequence;
	}
}

class Response {
	String timestamp;
	String status;
	String error;
	String message;
	String path;
	/**
	 * @return the timestamp
	 */
	public String getTimestamp() {
		return timestamp;
	}
	/**
	 * @param timestamp the timestamp to set
	 */
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
	/**
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}
	/**
	 * @param status the status to set
	 */
	public void setStatus(String status) {
		this.status = status;
	}
	/**
	 * @return the error
	 */
	public String getError() {
		return error;
	}
	/**
	 * @param error the error to set
	 */
	public void setError(String error) {
		this.error = error;
	}
	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}
	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}
	/**
	 * @return the path
	 */
	public String getPath() {
		return path;
	}
	/**
	 * @param path the path to set
	 */
	public void setPath(String path) {
		this.path = path;
	}
}

class RetrieveResponse {
	String accessionNumber;
	String hashKey;
	String wurcsLabel;
	
	/**
	 * @return the accessionNumber
	 */
	@JsonProperty("AccessionNumber")
	public String getAccessionNumber() {
		return accessionNumber;
	}
	/**
	 * @param accessionNumber the accessionNumber to set
	 */
	public void setAccessionNumber(String accessionNumber) {
		this.accessionNumber = accessionNumber;
	}
	/**
	 * @return the hashKey
	 */
	@JsonProperty("HashKey")
	public String getHashKey() {
		return hashKey;
	}
	/**
	 * @param hashKey the hashKey to set
	 */
	public void setHashKey(String hashKey) {
		this.hashKey = hashKey;
	}
	/**
	 * @return the wurcsLabel
	 */
	@JsonProperty("NormalizedWurcs")
	public String getWurcsLabel() {
		return wurcsLabel;
	}
	/**
	 * @param wurcsLabel the wurcsLabel to set
	 */
	public void setWurcsLabel(String wurcsLabel) {
		this.wurcsLabel = wurcsLabel;
	}	  
}

class GlytoucanResponse {
	String id;
	String wurcs;
	String message; // in case of error
	
	
	public String getId() {
		return id;
	}
	
	public String getWurcs() {
		return wurcs;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public void setWurcs(String wurcs) {
		this.wurcs = wurcs;
	}
	
	public String getMessage() {
        return message;
    }
	
	public void setMessage(String message) {
        this.message = message;
    }
}

class ValidationResponse {
    String m_sInputString;
    ValidationResult m_mapTypeToReports;
    String m_sStandardString;
    /**
     * @return the m_sInputString
     */
    public String getM_sInputString() {
        return m_sInputString;
    }
    /**
     * @param m_sInputString the m_sInputString to set
     */
    public void setM_sInputString(String m_sInputString) {
        this.m_sInputString = m_sInputString;
    }
    /**
     * @return the m_mapTypeToReports
     */
    public ValidationResult getM_mapTypeToReports() {
        return m_mapTypeToReports;
    }
    /**
     * @param m_mapTypeToReports the m_mapTypeToReports to set
     */
    public void setM_mapTypeToReports(ValidationResult m_mapTypeToReports) {
        this.m_mapTypeToReports = m_mapTypeToReports;
    }
    /**
     * @return the m_sStandardString
     */
    public String getM_sStandardString() {
        return m_sStandardString;
    }
    /**
     * @param m_sStandardString the m_sStandardString to set
     */
    public void setM_sStandardString(String m_sStandardString) {
        this.m_sStandardString = m_sStandardString;
    }
    
}

class ValidationResult {
    
    @JsonProperty ("ERROR")
    List<ValidationError> errors;

    /**
     * @return the errors
     */
    public List<ValidationError> getErrors() {
        return errors;
    }

    /**
     * @param errors the errors to set
     */
    public void setErrors(List<ValidationError> errors) {
        this.errors = errors;
    }
}

class ValidationError {
    String strMessage;
    ExceptionMessage exception;

    /**
     * @return the strMessage
     */
    public String getStrMessage() {
        return strMessage;
    }

    /**
     * @param strMessage the strMessage to set
     */
    public void setStrMessage(String strMessage) {
        this.strMessage = strMessage;
    }
    
    @Override
    public String toString() {
        return strMessage + " Exception: " + (exception == null ? "" :exception.toString()); 
    }

    /**
     * @return the exception
     */
    public ExceptionMessage getException() {
        return exception;
    }

    /**
     * @param exception the exception to set
     */
    public void setException(ExceptionMessage exception) {
        this.exception = exception;
    }
}

class ExceptionMessage {
    String m_strInput;
    String m_strMessage;
    
    @Override
    public String toString() {
        return m_strInput + " : " + m_strMessage;
    }

    /**
     * @return the m_strInput
     */
    public String getM_strInput() {
        return m_strInput;
    }

    /**
     * @param m_strInput the m_strInput to set
     */
    public void setM_strInput(String m_strInput) {
        this.m_strInput = m_strInput;
    }

    /**
     * @return the m_strMessage
     */
    public String getM_strMessage() {
        return m_strMessage;
    }

    /**
     * @param m_strMessage the m_strMessage to set
     */
    public void setM_strMessage(String m_strMessage) {
        this.m_strMessage = m_strMessage;
    }
}

class RegistrationErrorMessage {
	String timestamp;
	Integer status;
	String error;
	String message;
	String path;
	
	public String getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
	public Integer getStatus() {
		return status;
	}
	public void setStatus(Integer status) {
		this.status = status;
	}
	public String getError() {
		return error;
	}
	public void setError(String error) {
		this.error = error;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
}


class StatusResponse {
	String batch_p;
    String batch_value;
    
    @JsonProperty("batch_p")
	public String getBatch_p() {
		return batch_p;
	}
	public void setBatch_p(String batch_p) {
		this.batch_p = batch_p;
	}
	
	@JsonProperty("batch_value")
	public String getBatch_value() {
		return batch_value;
	}
	public void setBatch_value(String batch_value) {
		this.batch_value = batch_value;
	}
}
