package org.glygen.tablemaker.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;

import org.apache.commons.collections4.trie.PatriciaTrie;
import org.glygen.tablemaker.config.NamespaceHandler;
import org.glygen.tablemaker.persistence.FeedbackEntity;
import org.glygen.tablemaker.persistence.dao.FeedbackRepository;
import org.glygen.tablemaker.persistence.dao.LicenseRepository;
import org.glygen.tablemaker.persistence.dao.NamespaceRepository;
import org.glygen.tablemaker.persistence.dataset.License;
import org.glygen.tablemaker.persistence.dataset.Publication;
import org.glygen.tablemaker.persistence.glycan.Datatype;
import org.glygen.tablemaker.persistence.glycan.Metadata;
import org.glygen.tablemaker.persistence.glycan.Namespace;
import org.glygen.tablemaker.persistence.glycan.RegistrationStatus;
import org.glygen.tablemaker.persistence.table.GlycanColumns;
import org.glygen.tablemaker.service.EmailManager;
import org.glygen.tablemaker.util.pubmed.DOIUtil;
import org.glygen.tablemaker.util.pubmed.DTOPublication;
import org.glygen.tablemaker.util.pubmed.PubmedUtil;
import org.glygen.tablemaker.view.NamespaceEntry;
import org.glygen.tablemaker.view.SuccessResponse;
import org.slf4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/util")
public class UtilityController {
	
	private static final String m_strPubmedURL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pubmed&retmode=xml&id=";
	
	static Logger logger = org.slf4j.LoggerFactory.getLogger(UtilityController.class);
	
	private final NamespaceRepository namespaceRepository;
	private final FeedbackRepository feedbackRepository;
	private final EmailManager emailManager;
	private final LicenseRepository licenseRepository;
	
	public UtilityController(NamespaceRepository namespaceRepository, FeedbackRepository feedbackRepository, EmailManager emailManager, LicenseRepository licenseRepository) {
		this.namespaceRepository = namespaceRepository;
		this.feedbackRepository = feedbackRepository;
		this.emailManager = emailManager;
		this.licenseRepository = licenseRepository;
	}
	
	@Operation(summary = "Get all namespaces")
    @GetMapping("/namespaces")
	@ApiResponses (value ={
			@ApiResponse(responseCode="200", description="Return the namespaces"), 
            @ApiResponse(responseCode="500", description="Internal Server Error")
	})
    public ResponseEntity<SuccessResponse> getNamespaces() {
		return new ResponseEntity<> (new SuccessResponse (namespaceRepository.findAll(), "namespace list retrieved"), HttpStatus.OK);
	}
	
	@Operation(summary = "Get glycan registration status options")
    @GetMapping("/getregistrationstatuslist")
	@ApiResponses (value ={
			@ApiResponse(responseCode="200", description="Return the registration status options"), 
            @ApiResponse(responseCode="500", description="Internal Server Error")
	})
    public ResponseEntity<SuccessResponse> getRegistrationStatusList() {
		return new ResponseEntity<> (new SuccessResponse (RegistrationStatus.values(), "registration status list retrieved"), HttpStatus.OK);
	}
	
	@Operation(summary = "Get glycan related columns for tablemaker")
    @GetMapping("/getglycanmetadata")
	@ApiResponses (value ={
			@ApiResponse(responseCode="200", description="Return available glycan columns"), 
            @ApiResponse(responseCode="500", description="Internal Server Error")
	})
    public ResponseEntity<SuccessResponse> getGlycanMetadata() {
		List<Datatype> datatypes = new ArrayList<>();
		Long i = -1L;
		for (GlycanColumns col: GlycanColumns.values()) {
			Datatype type = new Datatype();
			type.setName(col.getLabel());
			type.setDescription(col.name());
			type.setDatatypeId(i--);
			datatypes.add(type);
		}
		
		return new ResponseEntity<> (new SuccessResponse (datatypes, "glycan metadata list retrieved"), HttpStatus.OK);
	}
	
	@Operation(summary = "Retrieve type ahead suggestions")
    @RequestMapping(value="/gettypeahead", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Return the matches, if any"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<SuccessResponse> getTypeAheadSuggestions (
            @Parameter(required=true, description="Name of the namespace to retrieve matches ")
            @RequestParam("namespace")
            String namespace, 
            @Parameter(required=true, description="value to match") 
            @RequestParam("value")
            String key, 
            @Parameter(required=false, description="limit of number of matches", example="10") 
            @RequestParam(name="limit", required=false)
            Integer limit) {
        
        namespace = namespace.trim();
        // find the file identifier associated with the given namespace
        Namespace entity = namespaceRepository.findByNameIgnoreCase(namespace);  
        PatriciaTrie<List<NamespaceEntry>> trie = null;
        List<String> suggestions = new ArrayList<>();
        if (entity.getFileIdentifier() != null) {
        	// find the exact match if exists and put it as the first proposal
            trie = NamespaceHandler.getTrieForNamespace(entity.getFileIdentifier());
            if (trie != null) {
            	suggestions = UtilityController.getSuggestions(trie, key, limit);
            } else {
            	logger.warn ("namespace file cannot be located: " + entity.getNamespaceId());
            }
        } 
        return new ResponseEntity<SuccessResponse>(new SuccessResponse(suggestions, "Suggestion found"), HttpStatus.OK);
    }
	
	@Operation(summary = "Retrieve canonical forms for given metadata")
    @RequestMapping(value="/getcanonicalform", method = RequestMethod.POST, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Return the canonical form, if any"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<SuccessResponse> getCanonicalForm ( 
    		@Parameter(required=true, description="Name of the namespace to retrieve matches ")
    		@RequestParam("namespace")
    		String namespace, 
    		@Parameter(required=true, description="value to match") 
            @RequestParam
            String value) {
		
		// find the file identifier associated with the given namespace
		Namespace entity = namespaceRepository.findByNameIgnoreCase(namespace);  
		List<NamespaceEntry> matches = new ArrayList<>();
		PatriciaTrie<List<NamespaceEntry>> trie = null;
		if (entity.getFileIdentifier() != null) {
			// find the exact match if exists
			trie = NamespaceHandler.getTrieForNamespace(entity.getFileIdentifier());
			if (trie != null) {
				Entry<String, List<NamespaceEntry>> entry = trie.select(value.toLowerCase());
				if (entry.getKey().toLowerCase().equals(value.toLowerCase())) {
					matches.addAll(entry.getValue());
				}
			}
		} else {
			/*NamespaceEntry entry = new NamespaceEntry();
			entry.setLabel(value);
        	matches.add(entry);*/
			logger.warn ("namespace file cannot be located: " + entity.getNamespaceId());
			
        }
		
		return new ResponseEntity<SuccessResponse>(new SuccessResponse(matches, "Canonical forms are returned, if any"), HttpStatus.OK);
		
	}
	
	@Operation(summary = "Retrieve canonical forms for given metadata")
    @RequestMapping(value="/getallcanonicalforms", method = RequestMethod.POST, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Replace metadata values with their canonical forms, if any"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<SuccessResponse> getAllCanonicalForms ( 
    		@RequestBody List<Metadata> metadata) {
		getCanonicalForm(namespaceRepository, metadata);
		return new ResponseEntity<SuccessResponse>(new SuccessResponse(metadata, "Canonical forms are replaced"), HttpStatus.OK);
	}
		
	
	
    public static void getCanonicalForm (NamespaceRepository namespaceRepository, Collection<Metadata> metadata) {
		for (Metadata meta: metadata) {
			String namespace = meta.getType().getNamespace().getName();
   
			// find the file identifier associated with the given namespace
			Namespace entity = namespaceRepository.findByNameIgnoreCase(namespace);  
			PatriciaTrie<List<NamespaceEntry>> trie = null;
			if (entity.getFileIdentifier() != null) {
				// find the exact match if exists
				trie = NamespaceHandler.getTrieForNamespace(entity.getFileIdentifier());
				if (trie != null) {
					Entry<String, List<NamespaceEntry>> entries = trie.select(meta.getValue().toLowerCase());
					if (entries.getKey().toLowerCase().equals(meta.getValue().toLowerCase())) {
						for (NamespaceEntry entry: entries.getValue()) {
							if (entry.getLabel().toLowerCase().equals(meta.getValue().toLowerCase())) {
								meta.setValue(entry.getLabel());
								meta.setValueUri(entry.getUri());
								if (meta.getValueUri() != null) {
			    					// last part of the uri is the id, either ../../<id> or ../../id=<id>
			    					if (meta.getValueUri().contains("id=")) {
			    						meta.setValueId (meta.getValueUri().substring(meta.getValueUri().indexOf("id=")+3));
			    					} else {
			    						meta.setValueId (meta.getValueUri().substring(meta.getValueUri().lastIndexOf("/")+1));
			    					}
			    				}
							}
						}
					}
				}
            } else {
            	logger.warn ("namespace file cannot be located: " + entity.getNamespaceId());
            }
		}
        
    }
	
    public static List<String> getSuggestions (PatriciaTrie<List<NamespaceEntry>> trie, String key, Integer limit) {
        Entry<String, List<NamespaceEntry>> entry = trie.select(key.toLowerCase());
        SortedMap<String, List<NamespaceEntry>> resultMap = trie.prefixMap(key.toLowerCase());
        List<String> result = new ArrayList<>();
        int i=0;
        if (entry != null && !resultMap.containsKey(entry.getKey())) {
            result.add(entry.getKey());
            i++;
        }
        for (Iterator<Entry<String, List<NamespaceEntry>>> iterator = resultMap.entrySet().iterator(); iterator.hasNext();) {
            Entry<String, List<NamespaceEntry>> match = iterator.next();
            if (limit != null && i >= limit)
                break;
            if (!result.contains(match.getKey())) {
            	result.add(match.getKey());
            	i++;
            }
        }
        
        return result;
    }
    
    @Operation(summary = "Check metadata validity")
    @PostMapping("/ismetadatavalid")
	public ResponseEntity<SuccessResponse> isMetadataValueValid (@Valid @RequestBody Metadata meta) {
		// check if the metadata value is valid based on its namespace
		boolean valid = true;
		String detailMessage = "valid";
		if (meta.getValue() == null || meta.getValue().isEmpty()) {
			valid = false;
			detailMessage = "value cannot be left blank";
		} else {
			Namespace namespace = meta.getType().getNamespace();
			if (namespace.getName().equals ("String")) {
				// do nothing
			}
			else if (namespace.getName().equals ("Integer")) {
				try {
					Integer.parseInt(meta.getValue());
				} catch (Exception e) {
					valid = false;
					detailMessage = "Value must be a number";
				}
			} else if (namespace.getName().equals ("Double")) {
				try {
					Double.parseDouble(meta.getValue());
				} catch (Exception e) {
					valid = false;
					detailMessage = "Value must be a number";
				}
			} else if (namespace.getName().equals ("Paper")) {
				if (meta.getValue().toLowerCase().startsWith("doi:")) {
					String[] parts = meta.getValue().substring (meta.getValue().lastIndexOf(":")+1).split("/");
					if (parts.length < 2) {
						valid = false;
						detailMessage = "Value must be a PMID or a DOI";
					} 
				} else {  //PMID
					try {
						int pmid = Integer.parseInt(meta.getValue());
						// check against pubmed
						valid = checkPubMed(pmid);
						if (!valid) {
							detailMessage = "PMID cannot be found in PubMed";
						}
					} catch (Exception e) {
						valid = false;
						detailMessage = "Value must be a PMID or a DOI";
				}
				}
			} else if (namespace.getName().equals ("Boolean")) {
				if (!meta.getValue().trim().equalsIgnoreCase("true") &&
					!meta.getValue().trim().equalsIgnoreCase("yes") &&
					!meta.getValue().trim().equalsIgnoreCase("false") &&
					!meta.getValue().trim().equalsIgnoreCase("no")) {
					valid = false;
					detailMessage = "Value must be one of true/false/yes/no";
				}
			} else if (namespace.getName().equals ("BCO contributor")) {
				String value = meta.getValue();
				String[] multiple = value.split("\\|");
				for (String c: multiple) {
					String regex1 = "(curatedBy|createdBy|authoredBy|contributedBy):.*\\(.*@.*,?\\s*.*\\)";
					String regex2 = "createdWith:.*\\(.*\\)";
					if (!c.matches(regex1) && !c.matches(regex2)) {
						valid = false;
						detailMessage = "Value must be in one of the following formats: curatedBy:Name(email,institution)\n"
								+ "\ncreatedWith:(software name,URL)"
								+ "\ncuratedBy can be replaced by createdBy/authoredBy/contributedBy";
					}
				}
				
			} else if (namespace.getFileIdentifier() != null) {
				PatriciaTrie<List<NamespaceEntry>> trie = NamespaceHandler.getTrieForNamespace(namespace.getFileIdentifier());
				Entry<String, List<NamespaceEntry>> entry = trie.select(meta.getValue().toLowerCase());
				if (entry == null || !entry.getKey().toLowerCase().equals(meta.getValue().toLowerCase())) {
					valid = false;
					detailMessage = "Value must be selected from suggestions";
				} 
			} else {
				// error
				throw new IllegalArgumentException ("Namespace is not valid for the given metadata");
			}
		}
		
		return new ResponseEntity<>(new SuccessResponse(valid, detailMessage), HttpStatus.OK);
	}
	
	boolean checkPubMed (Integer pmid) {
		try {
			URL pubmedURL = new URL(m_strPubmedURL + pmid);
			URLConnection t_connection = pubmedURL.openConnection();
	        t_connection.setUseCaches(false); 

	        BufferedReader t_reader = new BufferedReader(new InputStreamReader(t_connection.getInputStream()));
	        int t_count;
	        StringBuilder t_result = new StringBuilder();
	        while( (t_count = t_reader.read())!= -1 ) 
	        {
	            t_result.appendCodePoint(t_count);
	        }
	        return !t_result.isEmpty();
		} catch (IOException e) {
			return false;
		}
		
	}
	
	@Operation(summary = "Send the feedback about a page to the developers/administrators")
    @RequestMapping(value="/sendfeedback", method = RequestMethod.POST)
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="feedback stored and forwarded successfully", content = {
            @Content(schema = @Schema(implementation = String.class))}), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error for arguments"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<SuccessResponse> saveFeedback (
            @Parameter(required=true, description="feedback form") 
            @RequestBody @Valid FeedbackEntity feedback) {
        
        if (feedback == null) {
            throw new IllegalArgumentException("Feedback form is invalid");
        }
        
        feedbackRepository.save(feedback);
        emailManager.sendFeedbackNotice(feedback);
        List<String> emails = new ArrayList<String>();
        try {
            Resource classificationNamespace = new ClassPathResource("adminemails.txt");
            final InputStream inputStream = classificationNamespace.getInputStream();
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                emails.add(line.trim());
            }
        } catch (Exception e) {
            logger.error("Cannot load admin emails", e);
            throw new IllegalArgumentException("Feedback not sent! Cannot load admin emails");
        }
        emailManager.sendFeedback(feedback, emails.toArray(new String[0]));
        
        return new ResponseEntity<>(new SuccessResponse(feedback, "Feedback is sent"), HttpStatus.OK);
    }
	
	@Operation(summary = "Retrieve publication details from Pubmed with the given pubmed id")
    @RequestMapping(value="/getpublication", method = RequestMethod.GET)
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Publication retrieved successfully"), 
            @ApiResponse(responseCode="404", description="Publication with given id does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<SuccessResponse> getPublicationDetails (
            @Parameter(required=true, description="pubmed id or doi number for the publication", example="111 or doi:10.14454/FXWS-0523") 
            @RequestParam("identifier") String identifier) {
        if (identifier == null) {
            throw new IllegalArgumentException("Invalid Input: Not a valid publication information");
        }
        
        try {
        	return new ResponseEntity<>(new SuccessResponse(getPublication(identifier), "Publication retrieved"), HttpStatus.OK);
        } catch (Exception e) {    
            throw new IllegalArgumentException("Invalid Input: Not a valid publication information. Publication identifier is invalid");
        }
    }
	
	public static Publication getPublication (String identifier) throws Exception {
		
		if (identifier.toLowerCase().startsWith("doi:")) {
        	// retrieve by Doi number
        	String doiid = identifier.toLowerCase().substring(identifier.toLowerCase().indexOf("doi:")+4);
        	try {
        		DTOPublication pub = new DOIUtil().getPublication(doiid);
        		if (pub == null) {
        			pub = new DTOPublication();
        			pub.setDoiId(doiid);    // no other details can be retrieved
        		}
        		return getPublicationFrom(pub);
            } catch (Exception e) {    
                throw new IllegalArgumentException("Invalid Input: Not a valid publication information. Pubmed id is invalid");
            }
        } else {
        	try {
        		Integer pubmedid = Integer.parseInt(identifier);
        		PubmedUtil util = new PubmedUtil();
                try {
                    DTOPublication pub = util.createFromPubmedId(pubmedid);
                    if (pub == null) {
                    	pub = new DTOPublication();
                    	pub.setPubmedId(pubmedid);
                    }
                    getPublicationFrom(pub);
                } catch (Exception e) {    
                    throw new IllegalArgumentException("Invalid Input: Not a valid publication information. Pubmed id is invalid");
                }
        	} catch (NumberFormatException e) {
        		throw new IllegalArgumentException("Invalid Input: Not a valid publication information. Pubmed id is invalid");
        	}
        }
		return null;
	}
	
	public static Publication getPublicationFrom (DTOPublication pub) {
        Publication publication = new Publication ();
        publication.setAuthors(pub.getFormattedAuthor());
        publication.setDoiId(pub.getDoiId());
        publication.setEndPage(pub.getEndPage());
        publication.setJournal(pub.getJournal());
        publication.setNumber(pub.getNumber());
        publication.setPubmedId(pub.getPubmedId());
        publication.setStartPage(pub.getStartPage());
        publication.setTitle(pub.getTitle());
        publication.setVolume(pub.getVolume());
        publication.setYear(pub.getYear());
        
        return publication;
    }
	
	@Operation(summary="Retrieving license options")
    @RequestMapping(value="/licenses", method=RequestMethod.GET, 
            produces={"application/json"})
    @ApiResponses(value= {@ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<SuccessResponse> getLicenses(){
        List<License> allLicenses = licenseRepository.findAll();
		return new ResponseEntity<>(new SuccessResponse(allLicenses, "Licenses retrieved"), HttpStatus.OK);   
    }
}
