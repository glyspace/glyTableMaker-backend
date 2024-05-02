package org.glygen.tablemaker.controller;

import java.util.List;
import java.util.Optional;

import org.glygen.tablemaker.exception.BadRequestException;
import org.glygen.tablemaker.exception.DuplicateException;
import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.dao.DatatypeCategoryRepository;
import org.glygen.tablemaker.persistence.dao.DatatypeRepository;
import org.glygen.tablemaker.persistence.dao.UserRepository;
import org.glygen.tablemaker.persistence.glycan.Datatype;
import org.glygen.tablemaker.persistence.glycan.DatatypeCategory;
import org.glygen.tablemaker.persistence.glycan.Glycan;
import org.glygen.tablemaker.persistence.glycan.GlycanInCollection;
import org.glygen.tablemaker.service.MetadataManager;
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
	
	public MetadataController(DatatypeCategoryRepository datatypeCategoryRepository, UserRepository userRepository, DatatypeRepository datatypeRepository, MetadataManager metadataManager) {
		this.datatypeCategoryRepository = datatypeCategoryRepository;
		this.userRepository = userRepository;
		this.datatypeRepository = datatypeRepository;
		this.metadataManager = metadataManager;
	}
	
	@Operation(summary = "Get datatype categories", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/getcategories")
    public ResponseEntity<SuccessResponse> getCategories() {
    	
		List<DatatypeCategory> categories = datatypeCategoryRepository.findAll();
    	return new ResponseEntity<SuccessResponse>(
    			new SuccessResponse (categories, "datatype categories retrieved"), HttpStatus.OK);
    	
    }
	
	@Operation(summary = "Add datatype", security = { @SecurityRequirement(name = "bearer-key") })
    @PostMapping("/adddatatype")
    public ResponseEntity<SuccessResponse> addDatatype(
    		@Parameter(required=false, description="if provided, the created datatype is assigned to the given category")
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
    public ResponseEntity<SuccessResponse> updateCategory(@Valid @RequestBody DatatypeCategory d) {
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
	
}
