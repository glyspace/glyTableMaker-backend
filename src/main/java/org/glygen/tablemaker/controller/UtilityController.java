package org.glygen.tablemaker.controller;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.Map.Entry;

import org.apache.commons.collections4.trie.PatriciaTrie;
import org.glygen.tablemaker.config.NamespaceHandler;
import org.glygen.tablemaker.persistence.dao.NamespaceRepository;
import org.glygen.tablemaker.persistence.glycan.Namespace;
import org.glygen.tablemaker.view.NamespaceEntry;
import org.glygen.tablemaker.view.SuccessResponse;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/util")
public class UtilityController {
	
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
	
	@Operation(summary = "Retrieve type ahead suggestions")
    @RequestMapping(value="/gettypeahead", method = RequestMethod.GET, 
            produces={"application/json", "application/xml"})
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Return the matches, if any"), 
            @ApiResponse(responseCode="400", description="Invalid request, validation error"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<SuccessResponse> getTypeAheadSuggestions (
            @Parameter(required=true, description="Name of the namespace to retrieve matches "
                    + "(dataset, printedslide, pmid, username, organization, group, lastname, firstname, or any other namespace that is registered in the repository ontology)")
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
    
    

}
