package org.glygen.tablemaker.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.dao.DatasetRepository;
import org.glygen.tablemaker.persistence.dao.DatasetSpecification;
import org.glygen.tablemaker.persistence.dao.UserRepository;
import org.glygen.tablemaker.persistence.dataset.Dataset;
import org.glygen.tablemaker.persistence.dataset.DatasetError;
import org.glygen.tablemaker.persistence.dataset.DatasetVersion;
import org.glygen.tablemaker.view.CollectionView;
import org.glygen.tablemaker.view.DatasetInputView;
import org.glygen.tablemaker.view.DatasetView;
import org.glygen.tablemaker.view.Filter;
import org.glygen.tablemaker.view.Sorting;
import org.glygen.tablemaker.view.SuccessResponse;
import org.slf4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/dataset")
public class DatasetController {
	static Logger logger = org.slf4j.LoggerFactory.getLogger(DatasetController.class);
	
	final private DatasetRepository datasetRepository;
	final private UserRepository userRepository;
	
	public DatasetController(DatasetRepository datasetRepository, UserRepository userRepository) {
		this.datasetRepository = datasetRepository;
		this.userRepository = userRepository;
	}
	
	@Operation(summary = "Get user's public datasets", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/getdatasets")
    public ResponseEntity<SuccessResponse> getDatasets(
            @RequestParam("start")
            Integer start, 
            @RequestParam("size")
            Integer size,
            @RequestParam("filters")
            String filters,
            @RequestParam("globalFilter")
            String globalFilter,
            @RequestParam("sorting")
            String sorting) {
        // get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
      
        // parse filters and sorting
        ObjectMapper mapper = new ObjectMapper();
        List<Filter> filterList = null;
        if (filters != null && !filters.equals("[]")) {
            try {
                filterList = mapper.readValue(filters, 
                    new TypeReference<ArrayList<Filter>>() {});
            } catch (JsonProcessingException e) {
                throw new InternalError("filter parameter is invalid " + filters, e);
            }
        }
        List<Sorting> sortingList = null;
        List<Order> sortOrders = new ArrayList<>();
        if (sorting != null && !sorting.equals("[]")) {
            try {
                sortingList = mapper.readValue(sorting, 
                    new TypeReference<ArrayList<Sorting>>() {});
                for (Sorting s: sortingList) {
                    sortOrders.add(new Order(s.getDesc() ? Direction.DESC: Direction.ASC, s.getId()));
                }
            } catch (JsonProcessingException e) {
                throw new InternalError("sorting parameter is invalid " + sorting, e);
            }
        }
        
        // apply filters
        List<DatasetSpecification> specificationList = new ArrayList<>();
        if (filterList != null) {
	        for (Filter f: filterList) {
	        	DatasetSpecification spec = new DatasetSpecification(f);
	        	specificationList.add(spec);
	        }
        }
        
        if (globalFilter != null && !globalFilter.isBlank() && !globalFilter.equalsIgnoreCase("undefined")) {
        	specificationList.add(new DatasetSpecification(new Filter ("name", globalFilter)));
        	specificationList.add(new DatasetSpecification(new Filter ("datasetIdentifier", globalFilter)));
        }
        
        Specification<Dataset> spec = null;
        if (!specificationList.isEmpty()) {
        	spec = specificationList.get(0);
        	for (int i=1; i < specificationList.size(); i++) {
        		spec = Specification.where(spec).or(specificationList.get(i)); 
        	}
        	
        	spec = Specification.where(spec).and(DatasetSpecification.hasUserWithId(user.getUserId()));
        }
        
        Page<Dataset> datasetsInPage = null;
        if (spec != null) {
        	try {
        		datasetsInPage = datasetRepository.findAll(spec, PageRequest.of(start, size, Sort.by(sortOrders)));
        	} catch (Exception e) {
        		logger.error(e.getMessage(), e);
        		throw e;
        	}
        } else {
        	datasetsInPage = datasetRepository.findAllByUser(user, PageRequest.of(start, size, Sort.by(sortOrders)));
        }
        
        List<DatasetView> datasets = new ArrayList<>();
        for (Dataset d: datasetsInPage.getContent()) {
        	DatasetView dv = new DatasetView();
        	dv.setId(d.getDatasetId());
        	dv.setDatasetIdentifier(d.getDatasetIdentifier());
        	dv.setName(d.getName());
        	dv.setDescription(d.getDescription());
        	dv.setRetracted(d.getRetracted());
        	dv.setDateCreated(d.getDateCreated());
        	dv.setUser(d.getUser());
        	if (d.getAssociatedDatasources() != null) dv.setAssociatedDatasources(new ArrayList<>(d.getAssociatedDatasources()));
        	if (d.getGrants() != null) dv.setGrants(new ArrayList<>(d.getGrants()));
        	if (d.getAssociatedPapers() != null) dv.setAssociatedPapers(new ArrayList<>(d.getAssociatedPapers()));
        	if (d.getIntegratedIn() != null) dv.setIntegratedIn(new ArrayList<>(d.getIntegratedIn()));
        	if (d.getPublications() != null) dv.setPublications(new ArrayList<>(d.getPublications()));
        	
        	for (DatasetVersion version : d.getVersions()) {
        		if (version.getHead()) {
        			dv.setVersion(version.getVersion());
        			dv.setVersionDate(version.getVersionDate());
        			dv.setVersionComment(version.getComment());
        			if (version.getData() != null) dv.setData(new ArrayList<>(version.getData()));
        			if (version.getErrors() != null) dv.setErrors(new ArrayList<>(version.getErrors()));
        			break;
        		}
        	}
        	
        	datasets.add(dv);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("objects", datasets);
        response.put("currentPage", datasetsInPage.getNumber());
        response.put("totalItems", datasetsInPage.getTotalElements());
        response.put("totalPages", datasetsInPage.getTotalPages());
        
        return new ResponseEntity<>(new SuccessResponse(response, "datasets retrieved"), HttpStatus.OK);
    }
	
	@Operation(summary = "Publish dataset", security = { @SecurityRequirement(name = "bearer-key") })
    @PostMapping("/publishdataset")
    public ResponseEntity<SuccessResponse> publishDataset(@Valid @RequestBody DatasetInputView d) {
    	// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        
        //TODO save the dataset
        
        return new ResponseEntity<>(new SuccessResponse(d, "dataset has been published"), HttpStatus.OK);
	}
	@Operation(summary = "Get errors according to GlyGen template for the given collection", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/checkcollectionforerrors")
    public ResponseEntity<SuccessResponse> getDatasets(
            @RequestParam("collectionid")
            Long collectionId) {
		List<DatasetError> errorList = new ArrayList<>();
		//TODO check the collection's metadata and populate the errors and warnings
		
		return new ResponseEntity<>(new SuccessResponse(errorList, "errors/warnings retrieved"), HttpStatus.OK);
	}

}
