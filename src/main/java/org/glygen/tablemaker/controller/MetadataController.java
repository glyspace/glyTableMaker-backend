package org.glygen.tablemaker.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.trie.PatriciaTrie;
import org.glygen.tablemaker.config.NamespaceHandler;
import org.glygen.tablemaker.exception.DuplicateException;
import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.dao.DatatypeCategoryRepository;
import org.glygen.tablemaker.persistence.dao.DatatypeRepository;
import org.glygen.tablemaker.persistence.dao.UserRepository;
import org.glygen.tablemaker.persistence.glycan.Collection;
import org.glygen.tablemaker.persistence.glycan.Datatype;
import org.glygen.tablemaker.persistence.glycan.DatatypeCategory;
import org.glygen.tablemaker.persistence.glycan.Metadata;
import org.glygen.tablemaker.service.MetadataManager;
import org.glygen.tablemaker.view.NamespaceEntry;
import org.glygen.tablemaker.view.SuccessResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/metadata")
public class MetadataController {
	
	private final DatatypeCategoryRepository datatypeCategoryRepository;
	final private UserRepository userRepository;
	private final DatatypeRepository datatypeRepository;
	final private MetadataManager metadataManager;
	
	public MetadataController(DatatypeCategoryRepository datatypeCategoryRepository, 
			UserRepository userRepository, 
			DatatypeRepository datatypeRepository, 
			MetadataManager metadataManager) {
		this.datatypeCategoryRepository = datatypeCategoryRepository;
		this.userRepository = userRepository;
		this.datatypeRepository = datatypeRepository;
		this.metadataManager = metadataManager;
	}
	
	@Operation(summary = "Get datatype categories", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/getcategories")
    public ResponseEntity<SuccessResponse> getCategories() {
		// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
		List<DatatypeCategory> categories = datatypeCategoryRepository.findAll();
		List<DatatypeCategory> userCategories = new ArrayList<>();
		// only return the default one and the user's own
		for (DatatypeCategory c: categories) {
			if (c.getUser() == null || c.getUser().getUserId() == user.getUserId()) {
				userCategories.add(c);
				for (Datatype d: c.getDataTypes()) {
					if (d.getNamespace().getFileIdentifier() != null && !d.getNamespace().getHasId() && !d.getNamespace().getHasUri()) {
						// populate allowed values
						d.setAllowedValues(new ArrayList<>());
						PatriciaTrie<NamespaceEntry> trie = NamespaceHandler.getTrieForNamespace(d.getNamespace().getFileIdentifier());
						if (trie != null) {
							java.util.Collection<NamespaceEntry> allValues = trie.values();
							for (NamespaceEntry entry: allValues) {
								d.getAllowedValues().add(entry.getLabel());
							}
						}
					}
				}
			} 
		}
    	return new ResponseEntity<SuccessResponse>(
    			new SuccessResponse (userCategories, "datatype categories retrieved"), HttpStatus.OK);
    	
    }
	
	@Operation(summary = "Add datatype", security = { @SecurityRequirement(name = "bearer-key") })
    @PostMapping("/adddatatype")
    public ResponseEntity<SuccessResponse> addDatatype(
    		@Parameter(required=true, description="the created datatype is assigned to the given category")
    		@RequestParam("categoryid")
    		Long categoryId, 
    		@Valid @RequestBody Datatype d) {
    	// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        
        // check if duplicate
    	Datatype existing = datatypeRepository.findByNameIgnoreCaseAndUser(d.getName(), user);
    	if (existing != null) {
    		throw new DuplicateException("There is already a datatype with this name " + d.getName());
    	}
    	
    	d.setUser(user);
        d.setUri("https://www.glygen.org/datatype/");
    	
    	if (categoryId != null) {
    		Optional<DatatypeCategory> category = datatypeCategoryRepository.findById(categoryId);
    		DatatypeCategory cat = category.get();
    		d.setDatatypeId(null); // needs to be a new datatype
    		Datatype saved = metadataManager.addDatatypeToCategory (d, cat);
    		return new ResponseEntity<>(new SuccessResponse(saved, "datatype added to given category"), HttpStatus.OK);
    	} else {
	    	Datatype saved = datatypeRepository.save(d);
	    	saved.setUri(d.getUri()+saved.getDatatypeId());
	    	datatypeRepository.save(saved);
	    	return new ResponseEntity<>(new SuccessResponse(saved, "datatype added"), HttpStatus.OK);
    	}
    	
    }
	
	@Operation(summary = "Add datatype category", security = { @SecurityRequirement(name = "bearer-key") })
    @PostMapping("/addcategory")
    public ResponseEntity<SuccessResponse> addCategory(@Valid @RequestBody DatatypeCategory d) {
    	// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        
        // check if duplicate
    	DatatypeCategory existing = datatypeCategoryRepository.findByNameIgnoreCaseAndUser(d.getName(), user);
    	if (existing != null) {
    		throw new DuplicateException("There is already a datatype category with this name " + d.getName());
    	}
    	
        d.setUser(user);
    	DatatypeCategory saved = datatypeCategoryRepository.save(d);
    	return new ResponseEntity<>(new SuccessResponse(saved, "datatype category added"), HttpStatus.OK);
    }
	
	@Operation(summary = "Update datatype category", security = { @SecurityRequirement(name = "bearer-key") })
    @PostMapping("/updatecategory")
    public ResponseEntity<SuccessResponse> updateCategory(
    		@Valid @RequestBody DatatypeCategory d) {
    	// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        
        DatatypeCategory existing = null;
        if (d.getCategoryId() != null) {
        	Optional<DatatypeCategory> found = datatypeCategoryRepository.findById(d.getCategoryId());
        	existing = found.get();
        }
        
        if (!existing.getName().equalsIgnoreCase(d.getName())) {
	        // check if the name is duplicate
	    	DatatypeCategory sameName = datatypeCategoryRepository.findByNameIgnoreCaseAndUser(d.getName(), user);
	    	if (sameName != null && !sameName.getCategoryId().equals(d.getCategoryId())) {
	    		throw new DuplicateException("There is already a datatype category with this name " + d.getName());
	    	}
        }
    	
        existing.setDescription(d.getDescription());
        existing.setName(d.getName());
        existing.setDataTypes(d.getDataTypes());
    	DatatypeCategory saved = datatypeCategoryRepository.save(d);
    	return new ResponseEntity<>(new SuccessResponse(saved, "datatype category updated"), HttpStatus.OK);
    }
	
	@Operation(summary = "Update datatype", security = { @SecurityRequirement(name = "bearer-key") })
    @PostMapping("/updatedatatype")
    public ResponseEntity<SuccessResponse> updateDatatype(
    		@Parameter(required=false, description="the created datatype is assigned to the given category")
    		@RequestParam(required=false, name="categoryid")
    		Long categoryId, 
    		@Valid @RequestBody Datatype d) {
    	// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        
        Datatype existing = null;
        if (d.getDatatypeId() != null) {
        	Optional<Datatype> found = datatypeRepository.findById(d.getDatatypeId());
        	existing = found.get();
        }
        
        if (!existing.getName().equalsIgnoreCase(d.getName())) {
	        // check if the name is duplicate
	    	Datatype sameName = datatypeRepository.findByNameIgnoreCaseAndUser(d.getName(), user);
	    	if (sameName != null && !sameName.getDatatypeId().equals(d.getDatatypeId())) {
	    		throw new DuplicateException("There is already a datatype with this name " + d.getName());
	    	}
        }
    	
        existing.setDescription(d.getDescription());
        existing.setName(d.getName());
        existing.setMultiple(d.getMultiple());
        
        if (categoryId != null) {
        	// find existing datatype
        	List<DatatypeCategory> catList = datatypeCategoryRepository.findByDataTypes_datatypeId(existing.getDatatypeId());
        	DatatypeCategory existingCat = catList.get(0);
        	if (existingCat.getCategoryId() != categoryId) {
        		// changing the category
        		existingCat.getDataTypes().remove(existing);
        	}
    		Optional<DatatypeCategory> category = datatypeCategoryRepository.findById(categoryId);
    		DatatypeCategory cat = category.get();
    		Datatype saved = metadataManager.addDatatypeToCategory (existing, cat);
    		datatypeCategoryRepository.save(existingCat);
    		return new ResponseEntity<>(new SuccessResponse(saved, "datatype updated to given category"), HttpStatus.OK);
    	} else {
	    	datatypeRepository.save(existing);
	    	return new ResponseEntity<>(new SuccessResponse(existing, "datatype updated"), HttpStatus.OK);
    	}
    }
	
	@Operation(summary = "Delete given datatype category from the user's list", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/deletecategory/{categoryId}", method = RequestMethod.DELETE)
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Category deleted successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to delete categories"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<SuccessResponse> deleteCategory (
            @Parameter(required=true, description="internal id of the datatype category to delete") 
            @PathVariable("categoryId") Long categoryId) {
		
		if (categoryId == 1) {
			// cannot delete Glygen category
			throw new AccessDeniedException("Not allowed to delete default category");
		}
        
        // get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        Optional<DatatypeCategory> existing = datatypeCategoryRepository.findById(categoryId);
        if (existing.get() == null || existing.get().getUser() == null || !existing.get().getUser().getUserId().equals(user.getUserId())) {
            throw new IllegalArgumentException ("Could not find the given category " + categoryId + " for the user");
        }
        metadataManager.deleteDatatypeCategory(existing.get());
        return new ResponseEntity<>(new SuccessResponse(categoryId, "Category deleted successfully"), HttpStatus.OK);
    }
	
	@Operation(summary = "Delete given datatype from the user's list", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/deletedatatype/{datatypeId}", method = RequestMethod.DELETE)
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Datatype deleted successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to delete datatypes"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<SuccessResponse> deleteDatatype (
            @Parameter(required=true, description="internal id of the datatype to delete") 
            @PathVariable("datatypeId") Long datatypeId) {
		
		if (datatypeId < 17) {
			// cannot delete Glygen datatypes
			throw new AccessDeniedException("Not allowed to delete default datatypes");
		}
        
        // get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        Optional<Datatype> existing = datatypeRepository.findById(datatypeId);
        if (existing.get() == null || existing.get().getUser() == null || !existing.get().getUser().getUserId().equals(user.getUserId())) {
            throw new IllegalArgumentException ("Could not find the given datatype " + datatypeId + " for the user");
        }
        metadataManager.deleteDatatype(existing.get());
        return new ResponseEntity<>(new SuccessResponse(datatypeId, "Datatype deleted successfully"), HttpStatus.OK);
    }
	
	@Operation(summary = "Get datatype collections", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/getcollectionsfordatatype")
	public ResponseEntity<SuccessResponse> getCollectionsWithDatatype (
			@Parameter(required=true, description="datatype id")
    		@RequestParam("datatypeId")
    		Long datatypeId) {
		
		// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
		Optional<Datatype> existing = datatypeRepository.findById(datatypeId);
        if (existing.get() == null || existing.get().getUser() == null || !existing.get().getUser().getUserId().equals(user.getUserId())) {
            throw new IllegalArgumentException ("Could not find the given datatype " + datatypeId + " for the user");
        }
        List<Metadata> metadata = metadataManager.getMetadata(existing.get());
        List<Collection> collections = new ArrayList<>();
        for (Metadata m: metadata) {
        	collections.add(m.getCollection());
        }
        return new ResponseEntity<>(new SuccessResponse(collections, "Collections with the given datatype retrieved"), HttpStatus.OK);
	}
	
	
	
}
