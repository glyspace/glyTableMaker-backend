package org.glygen.tablemaker.controller;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;

import javax.imageio.ImageIO;

import org.apache.commons.collections4.trie.PatriciaTrie;
import org.glygen.tablemaker.config.NamespaceHandler;
import org.glygen.tablemaker.exception.DataNotFoundException;
import org.glygen.tablemaker.persistence.FeedbackEntity;
import org.glygen.tablemaker.persistence.GlycanImageEntity;
import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.dao.DatasetRepository;
import org.glygen.tablemaker.persistence.dao.FeedbackRepository;
import org.glygen.tablemaker.persistence.dao.GlycanImageRepository;
import org.glygen.tablemaker.persistence.dao.GlycanRepository;
import org.glygen.tablemaker.persistence.dao.GlycoproteinRepository;
import org.glygen.tablemaker.persistence.dao.LicenseRepository;
import org.glygen.tablemaker.persistence.dao.NamespaceRepository;
import org.glygen.tablemaker.persistence.dao.PublicationRepository;
import org.glygen.tablemaker.persistence.dao.UserRepository;
import org.glygen.tablemaker.persistence.dataset.License;
import org.glygen.tablemaker.persistence.dataset.Publication;
import org.glygen.tablemaker.persistence.glycan.Datatype;
import org.glygen.tablemaker.persistence.glycan.Glycan;
import org.glygen.tablemaker.persistence.glycan.Metadata;
import org.glygen.tablemaker.persistence.glycan.Namespace;
import org.glygen.tablemaker.persistence.glycan.RegistrationStatus;
import org.glygen.tablemaker.persistence.protein.GlycoproteinColumns;
import org.glygen.tablemaker.persistence.table.GlycanColumns;
import org.glygen.tablemaker.service.EmailManager;
import org.glygen.tablemaker.util.UniProtUtil;
import org.glygen.tablemaker.util.pubmed.DOIUtil;
import org.glygen.tablemaker.util.pubmed.DTOPublication;
import org.glygen.tablemaker.util.pubmed.PubmedUtil;
import org.glygen.tablemaker.view.GlycoproteinView;
import org.glygen.tablemaker.view.NamespaceEntry;
import org.glygen.tablemaker.view.StatisticsView;
import org.glygen.tablemaker.view.SuccessResponse;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/util")
public class UtilityController {
	
	private static final String m_strPubmedURL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pubmed&retmode=xml&id=";
	private static final String m_strDOIURL="https://doi.org/";
	
	static Logger logger = org.slf4j.LoggerFactory.getLogger(UtilityController.class);
	
	private final NamespaceRepository namespaceRepository;
	private final FeedbackRepository feedbackRepository;
	private final EmailManager emailManager;
	private final LicenseRepository licenseRepository;
	private final GlycanRepository glycanRepository;
	private final UserRepository userRepository;
	private final DatasetRepository datasetRepository;
	private final PublicationRepository publicationRepository;
	private final GlycanImageRepository glycanImageRepository;
	private final GlycoproteinRepository glycoproteinRepository;
	
	@Value("${spring.file.imagedirectory}")
    String imageLocation;
	
	public UtilityController(NamespaceRepository namespaceRepository, 
			FeedbackRepository feedbackRepository, 
			EmailManager emailManager, 
			LicenseRepository licenseRepository, 
			UserRepository userRepository, 
			GlycanRepository glycanRepository, 
			DatasetRepository datasetRepository, 
			PublicationRepository publicationRepository, 
			GlycanImageRepository glycanImageRepository, 
			GlycoproteinRepository glycoproteinRepository) {
		this.namespaceRepository = namespaceRepository;
		this.feedbackRepository = feedbackRepository;
		this.emailManager = emailManager;
		this.licenseRepository = licenseRepository;
		this.glycanRepository = glycanRepository;
		this.userRepository = userRepository;
		this.datasetRepository = datasetRepository;
		this.publicationRepository = publicationRepository;
		this.glycanImageRepository = glycanImageRepository;
		this.glycoproteinRepository = glycoproteinRepository;
	}
	
	@Operation(summary = "Get all namespaces")
    @GetMapping("/namespaces")
	@ApiResponses (value ={
			@ApiResponse(responseCode="200", description="Return the namespaces"), 
            @ApiResponse(responseCode="500", description="Internal Server Error")
	})
    public ResponseEntity<SuccessResponse<List<Namespace>>> getNamespaces() {
		return new ResponseEntity<> (new SuccessResponse<List<Namespace>> (namespaceRepository.findAll(), "namespace list retrieved"), HttpStatus.OK);
	}
	
	@Operation(summary = "Get glycan registration status options")
    @GetMapping("/getregistrationstatuslist")
	@ApiResponses (value ={
			@ApiResponse(responseCode="200", description="Return the registration status options"), 
            @ApiResponse(responseCode="500", description="Internal Server Error")
	})
    public ResponseEntity<SuccessResponse> getRegistrationStatusList() {
		return new ResponseEntity<> (new SuccessResponse<> (RegistrationStatus.values(), "registration status list retrieved"), HttpStatus.OK);
	}
	
	@Operation(summary = "Get glycan related columns for tablemaker")
    @GetMapping("/getglycanmetadata")
	@ApiResponses (value ={
			@ApiResponse(responseCode="200", description="Return available glycan columns"), 
            @ApiResponse(responseCode="500", description="Internal Server Error")
	})
    public ResponseEntity<SuccessResponse<List<Datatype>>> getGlycanMetadata() {
		List<Datatype> datatypes = new ArrayList<>();
		Long i = -1L;
		for (GlycanColumns col: GlycanColumns.values()) {
			Datatype type = new Datatype();
			type.setName(col.getLabel());
			type.setDescription(col.name());
			type.setDatatypeId(i--);
			datatypes.add(type);
		}
		
		return new ResponseEntity<> (new SuccessResponse<List<Datatype>> (datatypes, "glycan metadata list retrieved"), HttpStatus.OK);
	}
	
	@Operation(summary = "Get glycoprotein related columns for tablemaker")
    @GetMapping("/getglycoproteinmetadata")
	@ApiResponses (value ={
			@ApiResponse(responseCode="200", description="Return available glycoprotein columns"), 
            @ApiResponse(responseCode="500", description="Internal Server Error")
	})
    public ResponseEntity<SuccessResponse<List<Datatype>>> getGlycoproteinMetadata() {
		List<Datatype> datatypes = new ArrayList<>();
		Long i = -1L;
		for (GlycoproteinColumns col: GlycoproteinColumns.values()) {
			Datatype type = new Datatype();
			type.setName(col.getLabel());
			type.setDescription(col.name());
			type.setDatatypeId(i--);
			datatypes.add(type);
		}
		
		return new ResponseEntity<> (new SuccessResponse<List<Datatype>> (datatypes, "glycoprotein metadata list retrieved"), HttpStatus.OK);
	}
	
	@Operation(summary = "Retrieve type ahead suggestions")
    @RequestMapping(value="/gettypeahead", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Return the matches, if any"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<SuccessResponse<List<String>>> getTypeAheadSuggestions (
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
        PatriciaTrie<List<NamespaceEntry>> trie = null;
        
        if (namespace.equalsIgnoreCase("dataset")) {
            List<String> datasetNames = datasetRepository.getAllDatasetNames();
            trie = NamespaceHandler.createNamespaceFromList(datasetNames);
        } else if (namespace.equalsIgnoreCase("funding")) {
            List<String> fundingOrganizations = datasetRepository.getAllFundingOrganizations();
            trie = NamespaceHandler.createNamespaceFromList(fundingOrganizations);
        } else if (namespace.equalsIgnoreCase("organization")) {
            List<UserEntity> userList = userRepository.findAll();
            List<String> organizationNames = new ArrayList<String>();
            for (UserEntity user: userList) {
                if (user.getAffiliation() != null && !user.getAffiliation().isEmpty())
                    organizationNames.add(user.getAffiliation());
            }
            trie = NamespaceHandler.createNamespaceFromList(organizationNames);
        } else if (namespace.equalsIgnoreCase("group")) {
            List<UserEntity> userList = userRepository.findAll();
            List<String> groupNames = new ArrayList<String>();
            for (UserEntity user: userList) {
                if (user.getGroupName() != null && !user.getGroupName().isEmpty())
                    groupNames.add(user.getGroupName());
            }
            trie = NamespaceHandler.createNamespaceFromList(groupNames);
        } else if (namespace.equalsIgnoreCase("department")) {
            List<UserEntity> userList = userRepository.findAll();
            List<String> departments = new ArrayList<String>();
            for (UserEntity user: userList) {
                if (user.getDepartment() != null && !user.getDepartment().isEmpty())
                    departments.add(user.getDepartment());
            }
            trie = NamespaceHandler.createNamespaceFromList(departments);
        }else if (namespace.equalsIgnoreCase("lastname")) {
            List<UserEntity> userList = userRepository.findAll();
            List<String> lastNames = new ArrayList<String>();
            for (UserEntity user: userList) {
                if (user.getLastName() != null && !user.getLastName().isEmpty())
                    lastNames.add(user.getLastName());
            }
            trie = NamespaceHandler.createNamespaceFromList(lastNames);
        } else if (namespace.equalsIgnoreCase("firstname")) {
            List<UserEntity> userList = userRepository.findAll();
            List<String> firstNames = new ArrayList<String>();
            for (UserEntity user: userList) {
                if (user.getFirstName() != null && !user.getFirstName().isEmpty())
                    firstNames.add(user.getFirstName());
            }
            trie = NamespaceHandler.createNamespaceFromList(firstNames);
        } else {
            // find the file identifier associated with the given namespace
	        Namespace entity = namespaceRepository.findByNameIgnoreCase(namespace);  
	        if (entity.getFileIdentifier() != null) {
	        	// find the exact match if exists and put it as the first proposal
	            trie = NamespaceHandler.getTrieForNamespace(entity.getFileIdentifier());
	        } 
        }
        
        List<String> suggestions = new ArrayList<>();
        if (trie != null) {
        	suggestions = UtilityController.getSuggestions(trie, key, limit);
        } else {
        	logger.warn ("namespace file cannot be located for namespace: " + namespace);
        }
        
        return new ResponseEntity<>(new SuccessResponse<List<String>>(suggestions, "Suggestion found"), HttpStatus.OK);
    }
	
	@Operation(summary = "Retrieve canonical forms for given metadata")
    @RequestMapping(value="/getcanonicalform", method = RequestMethod.POST, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Return the canonical form, if any"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<SuccessResponse<List<NamespaceEntry>>> getCanonicalForm ( 
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
		
		return new ResponseEntity<>(new SuccessResponse<List<NamespaceEntry>>(matches, "Canonical forms are returned, if any"), HttpStatus.OK);
		
	}
	
	@Operation(summary = "Retrieve canonical forms for given metadata")
    @RequestMapping(value="/getallcanonicalforms", method = RequestMethod.POST, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Replace metadata values with their canonical forms, if any"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<SuccessResponse<List<Metadata>>> getAllCanonicalForms ( 
    		@RequestBody List<Metadata> metadata) {
		getCanonicalForm(namespaceRepository, metadata);
		return new ResponseEntity<>(new SuccessResponse<List<Metadata>>(metadata, "Canonical forms are replaced"), HttpStatus.OK);
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
	public ResponseEntity<SuccessResponse<Boolean>> isMetadataValueValid (@Valid @RequestBody Metadata meta) {
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
						detailMessage = "DOID is not valid. Example formats are 10.3390/cimb45110575 or doi:10.3390/cimb45110575 or https://doi.org/10.3390/cimb45110575";
					} else {
						try {
							valid = checkDOI(m_strDOIURL + meta.getValue().substring (meta.getValue().lastIndexOf(":")+1));
							if (!valid) {
								detailMessage = "DOID is not valid. Example formats are 10.3390/cimb45110575 or doi:10.3390/cimb45110575 or https://doi.org/10.3390/cimb45110575";
							}
						} catch (Exception e) {
							valid = false;
							detailMessage = "DOI " + meta.getValue() + " cannot be found";
						}
					}
				} else if (meta.getValue().toLowerCase().contains("doi.org")) {
					try {
						valid = checkDOI (meta.getValue());
						if (!valid) {
							detailMessage = "DOID is not valid. Example formats are 10.3390/cimb45110575 or doi:10.3390/cimb45110575 or https://doi.org/10.3390/cimb45110575";
						}
					} catch (Exception e) {
						valid = false;
						detailMessage = "DOI " + meta.getValue() + " cannot be found";
					}
				} else if (meta.getValue().toLowerCase().contains("/") && Character.isDigit(meta.getValue().charAt(0))) {
					try {	
						valid = checkDOI(m_strDOIURL + meta.getValue());
						if (!valid) {
							detailMessage = "DOID is not valid. Example formats are 10.3390/cimb45110575 or doi:10.3390/cimb45110575 or https://doi.org/10.3390/cimb45110575";
						}
					} catch (Exception e) {
						valid = false;
						detailMessage = "DOI " + meta.getValue() + " cannot be found";
					} 
				} else {  //PMID
					try {
						int pmid = Integer.parseInt(meta.getValue());
						// check against pubmed
						valid = checkPubMed(pmid);
						if (!valid) {
							detailMessage = "PMID " + meta.getValue() + " cannot be found in PubMed";
						}
					} catch (NumberFormatException e) {
						valid = false;
						detailMessage = "PMID must be an integer";
					} catch (Exception e) {
						valid = false;
						detailMessage = "Pubmed check failed. Reason: " + e.getMessage();
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
		
		return new ResponseEntity<>(new SuccessResponse<Boolean>(valid, detailMessage), HttpStatus.OK);
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
	
	boolean checkDOI (String doid) {
		try {
			URL pubmedURL = new URL(doid);
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
    public ResponseEntity<SuccessResponse<FeedbackEntity>> saveFeedback (
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
        
        return new ResponseEntity<>(new SuccessResponse<FeedbackEntity>(feedback, "Feedback is sent"), HttpStatus.OK);
    }
	
	@Operation(summary = "Retrieve publication details from Pubmed with the given pubmed id")
    @RequestMapping(value="/getpublication", method = RequestMethod.GET)
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Publication retrieved successfully"), 
            @ApiResponse(responseCode="404", description="Publication with given id does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<SuccessResponse<Publication>> getPublicationDetails (
            @Parameter(required=true, description="pubmed id or doi number for the publication", example="111 or doi:10.14454/FXWS-0523") 
            @RequestParam("identifier") String identifier) {
        if (identifier == null) {
            throw new IllegalArgumentException("Invalid Input: Not a valid publication information");
        }
        
        try {
        	ResponseEntity<SuccessResponse<Publication>> response = new ResponseEntity<>(new SuccessResponse<Publication>(getPublication(identifier, publicationRepository), "Publication retrieved"), HttpStatus.OK);
        	// put a sleep before next call
        	try {
		        Thread.sleep(400); // wait 100 milliseconds between requests
		    } catch (InterruptedException e) {
		        Thread.currentThread().interrupt(); // restore interrupted status
		    }
        	return response;
        } catch (Exception e) {    
            throw new IllegalArgumentException("Invalid Input: Not a valid publication information. Publication identifier is invalid");
        }
    }
	
	public static Publication getPublication (String identifier, PublicationRepository pubRepository) throws Exception {
		
		String doiid = null;
		
		if (identifier.toLowerCase().startsWith("doi:")) {
        	// retrieve by Doi number
        	doiid = identifier.toLowerCase().substring(identifier.toLowerCase().indexOf("doi:")+4);	
        } else if (identifier.toLowerCase().contains("doi.org")) {
        	doiid = identifier.toLowerCase().substring(identifier.toLowerCase().indexOf("doi.org") + 8);
        } else if (identifier.toLowerCase().contains("/") && Character.isDigit(identifier.charAt(0))) {
        	doiid = identifier.toLowerCase();
        }
		
		if (doiid != null) {
			try {
	    		List<Publication> existing = pubRepository.findByDoiId(doiid);
	    		if (existing != null && existing.size() > 0) {
	    			return existing.get(0);
	    		}
	    		DTOPublication pub = new DOIUtil().getPublication(doiid);
	    		if (pub == null) {
	    			pub = new DTOPublication();
	    			pub.setDoiId(doiid);    // no other details can be retrieved
	    		}
	    		return getPublicationFrom(pub);
	        } catch (Exception e) {    
	        	logger.warn("DOI retrieval failed", e);
	            throw new IllegalArgumentException("DOI retrieval failed. Reason:" + e.getMessage());
	        }
		} else {
        	try {
        		Integer pubmedid = Integer.parseInt(identifier);
        		PubmedUtil util = new PubmedUtil();
                try {
                	List<Publication> existing = pubRepository.findByPubmedId(pubmedid);
            		if (existing != null && existing.size() > 0) {
            			return existing.get(0);
            		}
                    DTOPublication pub = util.createFromPubmedId(pubmedid);
                    if (pub == null) {
                    	throw new IllegalArgumentException("Invalid Input: Not a valid publication information. Pubmed id is invalid");
                    }
                    return getPublicationFrom(pub);
                } catch (EntityNotFoundException e) {
                	throw new IllegalArgumentException("Invalid Input: Not a valid publication information. Pubmed id is invalid");
                } catch (Exception e) {
                	logger.warn("Pubmed retrieval failed", e);
                    throw new IllegalArgumentException("Pubmed retrieval failed. Please try again! Reason: " + e.getMessage());
                }
        	} catch (NumberFormatException e) {
        		throw new IllegalArgumentException("Invalid Input: Not a valid publication information. Pubmed id is invalid");
        	}
        }
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
    public ResponseEntity<SuccessResponse<List<License>>> getLicenses(){
        List<License> allLicenses = licenseRepository.findAll();
		return new ResponseEntity<>(new SuccessResponse<List<License>>(allLicenses, "Licenses retrieved"), HttpStatus.OK);   
    }
	
	@RequestMapping(value="/getstatistics", method=RequestMethod.GET)
    @Operation(summary="Retrieve the stats of the repository")
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Stats retrieved successfully", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = StatisticsView.class))}), 
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<SuccessResponse<StatisticsView>> getStatistics () {
        StatisticsView stats = new StatisticsView();
        stats.setUserCount(userRepository.count());
        stats.setDatasetCount(datasetRepository.count());
        stats.setGlycanCount((long)glycanRepository.findDistinctGlytoucanId().size());
        stats.setProteinCount((long)glycoproteinRepository.findDistinctUniprotId().size());
        stats.setNewGlycanCount(
        		glycanRepository.countByStatus(RegistrationStatus.NEWLY_REGISTERED) + 
        		glycanRepository.countByStatus(RegistrationStatus.NEWLY_SUBMITTED_FOR_REGISTRATION));
        return new ResponseEntity<>(new SuccessResponse<StatisticsView>(stats, "Statistics retrieved"), HttpStatus.OK); 
    }
	
	@Operation(summary="Retrieving all funding organizations from the repository", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/listfundingorganizations", method=RequestMethod.GET)
    @ApiResponses(value= {@ApiResponse(responseCode="200", description="list of existing funding organizations in the repository"),
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to retrieve funding organizations"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<SuccessResponse<Set<String>>> getFundingOrganizations(){
        Set<String> orgs = new HashSet<String>();
        orgs.add("NIH");
        orgs.add("FDA");
        orgs.add("DOI"); 
        // add from the datasets
        orgs.addAll(datasetRepository.getAllFundingOrganizations());
        if (!orgs.contains("Other")) {
            orgs.add("Other");
        }
        return new ResponseEntity<>(new SuccessResponse<Set<String>>(orgs, "Funding organizations retrieved"), HttpStatus.OK);
    }
	
	@Operation(summary = "Retrieve cartoon image for the glycan with the given glytoucan id")
    @RequestMapping(value="/getcartoon", method = RequestMethod.GET)
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Image retrieved successfully"), 
            @ApiResponse(responseCode="404", description="Image for the given glytoucan id  does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<SuccessResponse<byte[]>> getCartoon (
            @Parameter(required=true, description="GlyTouCan Id for the glycan") 
            @RequestParam("glytoucanId") String glytoucanId) {
        if (glytoucanId == null) {
            throw new IllegalArgumentException("Invalid Input: Not a valid glytoucan id");
        }

        return new ResponseEntity<>(new SuccessResponse<byte[]>(getCartoon (glytoucanId, glycanImageRepository, imageLocation), "Cartoon retrieved"), HttpStatus.OK);
    }
	
	public static byte[] getCartoon (String glytoucanId, GlycanImageRepository glycanImageRepository, String imageLocation) {
		try {
        	List<GlycanImageEntity> images = glycanImageRepository.findByGlytoucanId(glytoucanId.trim());
        	if (images != null && images.size() > 0) {
        		GlycanImageEntity image = images.get(0);
        		Long glycanId = images.get(0).getGlycanId();
        		byte[] cartoon = DataController.getImageForGlycan(imageLocation, glycanId);
        		if (cartoon == null) {
        			// generate and save
        			Glycan glycan = new Glycan();
        			glycan.setWurcs(image.getWurcs());
        			glycan.setGlytoucanID(image.getGlytoucanId());
        			BufferedImage t_image = DataController.createImageForGlycan(glycan);
        			if (t_image != null) {
                        String filename = glycanId + ".png";
                        //save the image into a file
                        logger.debug("Adding image to " + imageLocation);
                        File imageFile = new File(imageLocation + File.separator + filename);
                        try {
                            ImageIO.write(t_image, "png", imageFile);
                        } catch (IOException e) {
                            logger.error("could not write cartoon image to file", e);
                        }
                    } else {
                        logger.warn ("Glycan image cannot be generated for glycan " + glycanId);
                    }
                    GlycanImageEntity imageEntity = new GlycanImageEntity();
                    imageEntity.setGlycanId(glycanId);
                    imageEntity.setGlytoucanId(glycan.getGlytoucanID());
                    imageEntity.setWurcs(glycan.getWurcs());
                    glycanImageRepository.save(imageEntity);
        		}
            	return cartoon;
        		
        	} else {
        		throw new DataNotFoundException("Invalid Input: Glycan with the given glytoucan id cannot be located");
        	}
        	
        	
        } catch (Exception e) {    
        	logger.error("Error getting cartoon for " + glytoucanId, e);
            throw new IllegalArgumentException("Invalid Input: Not a valid glytoucan id");
        }
	}
	
	@Operation(summary = "Retrieve protein from UniProt with the given uniprot id")
    @RequestMapping(value="/getproteinfromuniprot/{uniprotid}", method = RequestMethod.GET)
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Protein retrieved successfully"), 
            @ApiResponse(responseCode="404", description="Sequence with given id does not exist"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<SuccessResponse<GlycoproteinView>> getSequenceFromUniProt (
            @Parameter(required=true, description="uniprotid such as P12345") 
            @PathVariable("uniprotid") String uniprotId,
            @Parameter(required=false, description="sequence version") 
            @RequestParam(name="version", required=false) String version) {
        if (uniprotId == null || uniprotId.trim().isEmpty()) {
            throw new IllegalArgumentException("uniprotId should be provided");
        }
        try {
            GlycoproteinView protein = UniProtUtil.getProteinFromUniProt(uniprotId.trim(), version == null ? null : version.trim());
            if (protein == null) {
                throw new DataNotFoundException("protein with the given UniProt ID " + uniprotId  + " cannot be found");
            }
            protein.setSites(new ArrayList<>());
            protein.setTags(new ArrayList<>());
            return new ResponseEntity<>(new SuccessResponse<GlycoproteinView>(protein, "Protein retrieved"), HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Could not retrieve from uniprot", e);
            throw new EntityNotFoundException("protein with the given UniProt ID " + uniprotId  + " cannot be found", e);
        }
    }
}
