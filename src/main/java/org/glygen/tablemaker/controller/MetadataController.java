package org.glygen.tablemaker.controller;

import java.util.List;

import org.glygen.tablemaker.exception.DuplicateException;
import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.dao.DatatypeCategoryRepository;
import org.glygen.tablemaker.persistence.dao.DatatypeRepository;
import org.glygen.tablemaker.persistence.dao.UserRepository;
import org.glygen.tablemaker.persistence.glycan.Datatype;
import org.glygen.tablemaker.persistence.glycan.DatatypeCategory;
import org.glygen.tablemaker.view.SuccessResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/metadata")
public class MetadataController {
	
	private final DatatypeCategoryRepository datatypeCategoryRepository;
	final private UserRepository userRepository;
	private final DatatypeRepository datatypeRepository;
	
	public MetadataController(DatatypeCategoryRepository datatypeCategoryRepository, UserRepository userRepository, DatatypeRepository datatypeRepository) {
		this.datatypeCategoryRepository = datatypeCategoryRepository;
		this.userRepository = userRepository;
		this.datatypeRepository = datatypeRepository;
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
    public ResponseEntity<SuccessResponse> addDatatype(@Valid @RequestBody Datatype d) {
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
    	Datatype saved = datatypeRepository.save(d);
    	saved.setUri(d.getUri()+saved.getDatatypeId());
    	datatypeRepository.save(saved);
    	return new ResponseEntity<>(new SuccessResponse(saved, "datatype added"), HttpStatus.OK);
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

}
