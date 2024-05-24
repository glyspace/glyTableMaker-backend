package org.glygen.tablemaker.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.Map.Entry;

import org.apache.commons.collections4.trie.PatriciaTrie;
import org.glygen.tablemaker.config.NamespaceHandler;
import org.glygen.tablemaker.persistence.dao.NamespaceRepository;
import org.glygen.tablemaker.persistence.glycan.Datatype;
import org.glygen.tablemaker.persistence.glycan.Metadata;
import org.glygen.tablemaker.persistence.glycan.Namespace;
import org.glygen.tablemaker.persistence.table.GlycanColumns;
import org.glygen.tablemaker.view.NamespaceEntry;
import org.glygen.tablemaker.view.SuccessResponse;
import org.slf4j.Logger;
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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/util")
public class UtilityController {
	
	private static final String m_strPubmedURL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pubmed&retmode=xml&id=";
	
	static Logger logger = org.slf4j.LoggerFactory.getLogger(UtilityController.class);
	
	private final NamespaceRepository namespaceRepository;
	
	public UtilityController(NamespaceRepository namespaceRepository) {
		this.namespaceRepository = namespaceRepository;
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
        PatriciaTrie<NamespaceEntry> trie = null;
        List<NamespaceEntry> suggestions = new ArrayList<>();
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
    
    public static List<NamespaceEntry> getSuggestions (PatriciaTrie<NamespaceEntry> trie, String key, Integer limit) {
        //Entry<String, NamespaceEntry> entry = trie.select(key.toLowerCase());
        SortedMap<String, NamespaceEntry> resultMap = trie.prefixMap(key.toLowerCase());
        List<NamespaceEntry> result = new ArrayList<>();
        int i=0;
       /* if (entry != null && !resultMap.containsValue(entry.getValue())) {
            result.add(entry.getValue());
            i++;
        }
        */   // do not put the best match
        for (Iterator<Entry<String, NamespaceEntry>> iterator = resultMap.entrySet().iterator(); iterator.hasNext();) {
            Entry<String, NamespaceEntry> match = iterator.next();
            if (limit != null && i >= limit)
                break;
            result.add(match.getValue());
            i++;
        }
        
        return result;
    }
    
    @Operation(summary = "Check metadata validity", security = { @SecurityRequirement(name = "bearer-key") })
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
					String regex1 = "(curatedBy|createdBy|authoredBy|contributedBy):.*\\(.*@.*,\\s*.*\\)";
					String regex2 = "createdWith:.*\\(.*\\)";
					if (!c.matches(regex1) && !c.matches(regex2)) {
						valid = false;
						detailMessage = "Value must be in one of the following formats: curatedBy:Name(email,institution)\n"
								+ "\ncreatedWith:(software name,URL)"
								+ "\ncuratedBy can be replaced by createdBy/authoredBy/contributedBy";
					}
				}
				
			} else if (namespace.getFileIdentifier() != null) {
				PatriciaTrie<NamespaceEntry> trie = NamespaceHandler.getTrieForNamespace(namespace.getFileIdentifier());
				Entry<String, NamespaceEntry> entry = trie.select(meta.getValue().toLowerCase());
				if (entry == null) {
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

}
