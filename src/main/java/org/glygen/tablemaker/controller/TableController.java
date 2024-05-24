package org.glygen.tablemaker.controller;

import java.util.List;

import org.glygen.tablemaker.exception.DuplicateException;
import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.dao.TemplateRepository;
import org.glygen.tablemaker.persistence.dao.UserRepository;
import org.glygen.tablemaker.persistence.glycan.DatatypeCategory;
import org.glygen.tablemaker.persistence.table.TableMakerTemplate;
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
@RequestMapping("/api/table")
public class TableController {
	
	final private UserRepository userRepository;
	final private TemplateRepository templateRepository;
	
	public TableController(UserRepository userRepository, TemplateRepository templateRepository) {
		this.userRepository = userRepository;
		this.templateRepository = templateRepository;
	}
	
	@Operation(summary = "Get all templates for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/gettemplates")
    public ResponseEntity<SuccessResponse> getCategories() {
		// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
		List<TableMakerTemplate> templates = templateRepository.findAllByUser(user);
    	return new ResponseEntity<SuccessResponse>(
    			new SuccessResponse (templates, "templates retrieved"), HttpStatus.OK);
    	
    }
	
	@Operation(summary = "Add table maker template for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @PostMapping("/addtemplate")
    public ResponseEntity<SuccessResponse> addTemplate(
    		@Valid @RequestBody TableMakerTemplate template) {
    	// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        
        // check if duplicate
    	TableMakerTemplate existing = templateRepository.findByNameIgnoreCaseAndUser(template.getName(), user);
    	if (existing != null) {
    		throw new DuplicateException("There is already a template with this name " + template.getName());
    	}
    	
    	template.setUser(user);
    	TableMakerTemplate saved = templateRepository.save(template);
    	return new ResponseEntity<>(new SuccessResponse(saved, "template added"), HttpStatus.OK);
	}
}
