package org.glygen.tablemaker.controller;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.eurocarbdb.MolecularFramework.io.GlycoCT.SugarExporterGlycoCTCondensed;
import org.eurocarbdb.MolecularFramework.io.GlycoCT.SugarImporterGlycoCTCondensed;
import org.eurocarbdb.MolecularFramework.sugar.Sugar;
import org.eurocarbdb.MolecularFramework.util.analytical.mass.GlycoVisitorMass;
import org.eurocarbdb.MolecularFramework.util.visitor.GlycoVisitorException;
import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.eurocarbdb.application.glycanbuilder.util.GraphicOptions;
import org.glycoinfo.GlycanCompositionConverter.conversion.CompositionConverter;
import org.glycoinfo.GlycanCompositionConverter.conversion.ConversionException;
import org.glycoinfo.GlycanCompositionConverter.structure.Composition;
import org.glycoinfo.GlycanCompositionConverter.utils.CompositionParseException;
import org.glycoinfo.GlycanCompositionConverter.utils.CompositionUtils;
import org.glycoinfo.GlycanCompositionConverter.utils.DictionaryException;
import org.glycoinfo.WURCSFramework.util.validation.WURCSValidator;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.glygen.tablemaker.exception.BadRequestException;
import org.glygen.tablemaker.exception.BatchUploadException;
import org.glygen.tablemaker.exception.DataNotFoundException;
import org.glygen.tablemaker.exception.DuplicateException;
import org.glygen.tablemaker.persistence.BatchUploadEntity;
import org.glygen.tablemaker.persistence.UploadErrorEntity;
import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.dao.BatchUploadRepository;
import org.glygen.tablemaker.persistence.dao.CollectionRepository;
import org.glygen.tablemaker.persistence.dao.CollectionSpecification;
import org.glygen.tablemaker.persistence.dao.GlycanRepository;
import org.glygen.tablemaker.persistence.dao.GlycanSpecifications;
import org.glygen.tablemaker.persistence.dao.UploadErrorRepository;
import org.glygen.tablemaker.persistence.dao.UserRepository;
import org.glygen.tablemaker.persistence.glycan.Collection;
import org.glygen.tablemaker.persistence.glycan.Glycan;
import org.glygen.tablemaker.persistence.glycan.GlycanInCollection;
import org.glygen.tablemaker.persistence.glycan.GlycanInFile;
import org.glygen.tablemaker.persistence.glycan.GlycanTag;
import org.glygen.tablemaker.persistence.glycan.RegistrationStatus;
import org.glygen.tablemaker.persistence.glycan.UploadStatus;
import org.glygen.tablemaker.service.AsyncService;
import org.glygen.tablemaker.service.EmailManager;
import org.glygen.tablemaker.service.GlycanManagerImpl;
import org.glygen.tablemaker.util.FixGlycoCtUtil;
import org.glygen.tablemaker.util.GlytoucanUtil;
import org.glygen.tablemaker.util.SequenceUtils;
import org.glygen.tablemaker.view.CollectionView;
import org.glygen.tablemaker.view.FileWrapper;
import org.glygen.tablemaker.view.Filter;
import org.glygen.tablemaker.view.GlycanView;
import org.glygen.tablemaker.view.SequenceFormat;
import org.glygen.tablemaker.view.Sorting;
import org.glygen.tablemaker.view.SuccessResponse;
import org.glygen.tablemaker.view.UserStatisticsView;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
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
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/data")
public class DataController {
    
    static Logger logger = org.slf4j.LoggerFactory.getLogger(DataController.class);
    static BuilderWorkspace glycanWorkspace = new BuilderWorkspace(new GlycanRendererAWT());
    static {       
            glycanWorkspace.initData();
            // Set orientation of glycan: RL - right to left, LR - left to right, TB - top to bottom, BT - bottom to top
            glycanWorkspace.getGraphicOptions().ORIENTATION = GraphicOptions.RL;
            // Set flag to show information such as linkage positions and anomers
            glycanWorkspace.getGraphicOptions().SHOW_INFO = true;
            // Set flag to show mass
            glycanWorkspace.getGraphicOptions().SHOW_MASSES = false;
            // Set flag to show reducing end
            glycanWorkspace.getGraphicOptions().SHOW_REDEND = true;

            glycanWorkspace.setDisplay(GraphicOptions.DISPLAY_NORMALINFO);
            glycanWorkspace.setNotation(GraphicOptions.NOTATION_SNFG);
    }
    
    final private GlycanRepository glycanRepository;
    final private CollectionRepository collectionRepository;
    final private UserRepository userRepository;
    final private BatchUploadRepository uploadRepository;
    final private AsyncService batchUploadService;
    final private GlycanManagerImpl glycanManager;
    final private UploadErrorRepository uploadErrorRepository;
    private final EmailManager emailManager;
    
    @Value("${spring.file.imagedirectory}")
    String imageLocation;
    
    @Value("${spring.file.uploaddirectory}")
	String uploadDir;
    
    public DataController(GlycanRepository glycanRepository, UserRepository userRepository,
    		BatchUploadRepository uploadRepository, AsyncService uploadService, 
    		CollectionRepository collectionRepository, GlycanManagerImpl glycanManager, 
    		UploadErrorRepository uploadErrorRepository, EmailManager emailManager) {
        this.glycanRepository = glycanRepository;
		this.collectionRepository = collectionRepository;
        this.userRepository = userRepository;
        this.uploadRepository = uploadRepository;
		this.batchUploadService = uploadService;
		this.glycanManager = glycanManager;
		this.uploadErrorRepository = uploadErrorRepository;
		this.emailManager = emailManager;
    }
    
    @Operation(summary = "Get data counts", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/statistics")
    public ResponseEntity<SuccessResponse> getStatistics() {
        UserStatisticsView stats = new UserStatisticsView();
     // get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        stats.setGlycanCount(glycanRepository.count(GlycanSpecifications.hasUserWithId(user.getUserId())));
        Specification<Collection> spec = CollectionSpecification.hasUserWithId(user.getUserId());
    	spec = Specification.where(spec).and(CollectionSpecification.hasNoChildren());
        stats.setCollectionCount(collectionRepository.count(spec));
        spec = CollectionSpecification.hasUserWithId(user.getUserId());
    	spec = Specification.where(spec).and(CollectionSpecification.hasChildren());
    	stats.setCocCount(collectionRepository.count(spec));
        return new ResponseEntity<>(new SuccessResponse(stats, "statistics gathered"), HttpStatus.OK);
    }
    
    @Operation(summary = "Get glycans", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/getglycans")
    public ResponseEntity<SuccessResponse> getGlycans(
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
        List<GlycanSpecifications> specificationList = new ArrayList<>();
        if (filterList != null) {
	        for (Filter f: filterList) {
	        	GlycanSpecifications spec = new GlycanSpecifications(f);
	        	specificationList.add(spec);
	        }
        }
        
        if (globalFilter != null && !globalFilter.isBlank() && !globalFilter.equalsIgnoreCase("undefined")) {
        	specificationList.add(new GlycanSpecifications(new Filter ("glytoucanID", globalFilter)));
        	specificationList.add(new GlycanSpecifications(new Filter ("mass", globalFilter)));
        }
        
        Specification<Glycan> spec = null;
        if (!specificationList.isEmpty()) {
        	spec = specificationList.get(0);
        	for (int i=1; i < specificationList.size(); i++) {
        		spec = Specification.where(spec).or(specificationList.get(i)); 
        	}
        	spec = Specification.where(spec).or(GlycanSpecifications.hasGlycanTag(globalFilter));   // add glycan tag to searchable list
        	spec = Specification.where(spec).and(GlycanSpecifications.hasUserWithId(user.getUserId()));
        }
        
        Page<Glycan> glycansInPage = null;
        if (spec != null) {
        	try {
        		glycansInPage = glycanRepository.findAll(spec, PageRequest.of(start, size, Sort.by(sortOrders)));
        	} catch (Exception e) {
        		logger.error(e.getMessage(), e);
        		throw e;
        	}
        } else {
        	glycansInPage = glycanRepository.findAllByUser(user, PageRequest.of(start, size, Sort.by(sortOrders)));
        }
        
        // retrieve cartoon images
        for (Glycan g: glycansInPage.getContent()) {
            try {
                g.setCartoon(getImageForGlycan(g.getGlycanId()));
                if (g.getGlytoucanID() == null && g.getGlytoucanHash() != null && g.getWurcs() != null) {
                	// registered, try to get the accession number
                	g.setGlytoucanID(GlytoucanUtil.getInstance().getAccessionNumber(g.getWurcs()));
                }
            } catch (DataNotFoundException e) {
                // ignore
                logger.warn ("no image found for glycan " + g.getGlycanId());
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("objects", glycansInPage.getContent());
        response.put("currentPage", glycansInPage.getNumber());
        response.put("totalItems", glycansInPage.getTotalElements());
        response.put("totalPages", glycansInPage.getTotalPages());
        
        return new ResponseEntity<>(new SuccessResponse(response, "glycans retrieved"), HttpStatus.OK);
    }
    
    @Operation(summary = "Get collections", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/getcollections")
    public ResponseEntity<SuccessResponse> getCollections(
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
        List<CollectionSpecification> specificationList = new ArrayList<>();
        if (filterList != null) {
	        for (Filter f: filterList) {
	        	CollectionSpecification spec = new CollectionSpecification(f);
	        	specificationList.add(spec);
	        }
        }
        
        if (globalFilter != null && !globalFilter.isBlank() && !globalFilter.equalsIgnoreCase("undefined")) {
        	specificationList.add(new CollectionSpecification(new Filter ("name", globalFilter)));
        	specificationList.add(new CollectionSpecification(new Filter ("description", globalFilter)));
        }
        
        Specification<Collection> spec = null;
        if (!specificationList.isEmpty()) {
        	spec = specificationList.get(0);
        	for (int i=1; i < specificationList.size(); i++) {
        		spec = Specification.where(spec).or(specificationList.get(i)); 
        	}
        	
        	spec = Specification.where(spec).and(CollectionSpecification.hasUserWithId(user.getUserId()));
        	spec = Specification.where(spec).and(CollectionSpecification.hasNoChildren());
        }
        
        Page<Collection> collectionsInPage = null;
        if (spec != null) {
        	try {
        		collectionsInPage = collectionRepository.findAll(spec, PageRequest.of(start, size, Sort.by(sortOrders)));
        	} catch (Exception e) {
        		logger.error(e.getMessage(), e);
        		throw e;
        	}
        } else {
        	collectionsInPage = collectionRepository.findNonParentCollectionsByUser(user, PageRequest.of(start, size, Sort.by(sortOrders)));
        }
        
        List<CollectionView> collections = new ArrayList<>();
        for (Collection c: collectionsInPage.getContent()) {
        	CollectionView cv = new CollectionView();
        	cv.setCollectionId(c.getCollectionId());
        	cv.setName(c.getName());
        	cv.setDescription(c.getDescription());
        	if (c.getMetadata() != null) cv.setMetadata(new ArrayList<>(c.getMetadata()));
        	if (c.getTags() != null) cv.setTags(new ArrayList<>(c.getTags()));
        	cv.setGlycans(new ArrayList<Glycan>());
        	for (GlycanInCollection gic: c.getGlycans()) {
        		Glycan g = gic.getGlycan();
        		g.setGlycanCollections(null);
        		try {
                    g.setCartoon(getImageForGlycan(g.getGlycanId()));
                } catch (DataNotFoundException e) {
                    // ignore
                    logger.warn ("no image found for glycan " + g.getGlycanId());
                }
        		cv.getGlycans().add(g);
        	}
        	collections.add(cv);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("objects", collections);
        response.put("currentPage", collectionsInPage.getNumber());
        response.put("totalItems", collectionsInPage.getTotalElements());
        response.put("totalPages", collectionsInPage.getTotalPages());
        
        return new ResponseEntity<>(new SuccessResponse(response, "collections retrieved"), HttpStatus.OK);
    }
    
    @Operation(summary = "Get collections of collections", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/getcoss")
    public ResponseEntity<SuccessResponse> getCollectionsOfCollections(
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
        List<CollectionSpecification> specificationList = new ArrayList<>();
        if (filterList != null) {
	        for (Filter f: filterList) {
	        	CollectionSpecification spec = new CollectionSpecification(f);
	        	specificationList.add(spec);
	        }
        }
        
        if (globalFilter != null && !globalFilter.isBlank() && !globalFilter.equalsIgnoreCase("undefined")) {
        	specificationList.add(new CollectionSpecification(new Filter ("name", globalFilter)));
        	specificationList.add(new CollectionSpecification(new Filter ("description", globalFilter)));
        }
        
        Specification<Collection> spec = null;
        if (!specificationList.isEmpty()) {
        	spec = specificationList.get(0);
        	for (int i=1; i < specificationList.size(); i++) {
        		spec = Specification.where(spec).or(specificationList.get(i)); 
        	}
        	
        	spec = Specification.where(spec).and(CollectionSpecification.hasUserWithId(user.getUserId()));
        	spec = Specification.where(spec).and(CollectionSpecification.hasChildren());
        }
        
        Page<Collection> collectionsInPage = null;
        if (spec != null) {
        	try {
        		collectionsInPage = collectionRepository.findAll(spec, PageRequest.of(start, size, Sort.by(sortOrders)));
        	} catch (Exception e) {
        		logger.error(e.getMessage(), e);
        		throw e;
        	}
        } else {
        	collectionsInPage = collectionRepository.findParentCollectionsByUser(user, PageRequest.of(start, size, Sort.by(sortOrders)));
        }
        
        List<CollectionView> collections = new ArrayList<>();
        for (Collection c: collectionsInPage.getContent()) {
        	CollectionView cv = new CollectionView();
        	cv.setCollectionId(c.getCollectionId());
        	cv.setName(c.getName());
        	cv.setDescription(c.getDescription());
        	if (c.getTags() != null) cv.setTags(new ArrayList<>(c.getTags()));
        	cv.setChildren(new ArrayList<>());
        	for (Collection cc: c.getCollections()) {
        		CollectionView child = new CollectionView();
        		child.setCollectionId(cc.getCollectionId());
        		child.setName(cc.getName());
        		child.setDescription(cc.getDescription());
        		if (cc.getMetadata() != null) child.setMetadata(new ArrayList<>(cc.getMetadata()));
        		if (cc.getTags() != null) child.setTags(new ArrayList<>(cc.getTags()));
        		child.setGlycans(new ArrayList<Glycan>());
            	for (GlycanInCollection gic: cc.getGlycans()) {
            		Glycan g = gic.getGlycan();
            		g.setGlycanCollections(null);
            		try {
                        g.setCartoon(getImageForGlycan(g.getGlycanId()));
                    } catch (DataNotFoundException e) {
                        // ignore
                        logger.warn ("no image found for glycan " + g.getGlycanId());
                    }
            		child.getGlycans().add(g);
            	}
        		cv.getChildren().add(child);
        	}
        	collections.add(cv);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("objects", collections);
        response.put("currentPage", collectionsInPage.getNumber());
        response.put("totalItems", collectionsInPage.getTotalElements());
        response.put("totalPages", collectionsInPage.getTotalPages());
        
        return new ResponseEntity<>(new SuccessResponse(response, "collections of collections retrieved"), HttpStatus.OK);
    }
    
    
    @Operation(summary = "Get collection by the given id", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/getcollection/{collectionId}")
    public ResponseEntity<SuccessResponse> getCollectionById(
    		@Parameter(required=true, description="id of the collection to be retrieved") 
    		@PathVariable("collectionId") Long collectionId) {
    	// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        Collection existing = collectionRepository.findByCollectionIdAndUser(collectionId, user);
        if (existing == null) {
            throw new IllegalArgumentException ("Could not find the given collection " + collectionId + " for the user");
        }
        
        CollectionView cv = new CollectionView();
        cv.setCollectionId(existing.getCollectionId());
    	cv.setName(existing.getName());
    	cv.setDescription(existing.getDescription());
    	if (existing.getMetadata() != null) cv.setMetadata(new ArrayList<>(existing.getMetadata()));
    	if (existing.getTags() != null) cv.setTags(new ArrayList<>(existing.getTags()));
    	cv.setGlycans(new ArrayList<Glycan>());
    	for (GlycanInCollection gic: existing.getGlycans()) {
    		Glycan g = gic.getGlycan();
    		g.setGlycanCollections(null);
    		try {
                g.setCartoon(getImageForGlycan(g.getGlycanId()));
            } catch (DataNotFoundException e) {
                // ignore
                logger.warn ("no image found for glycan " + g.getGlycanId());
            }
    		cv.getGlycans().add(g);
    	}
        
        return new ResponseEntity<>(new SuccessResponse(cv, "collection retrieved"), HttpStatus.OK);
    }
    
    @Operation(summary = "Get collection of collections by the given id", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/getcoc/{collectionId}")
    public ResponseEntity<SuccessResponse> getCoCById(
    		@Parameter(required=true, description="id of the collection to be retrieved") 
    		@PathVariable("collectionId") Long collectionId) {
    	// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        Collection existing = collectionRepository.findByCollectionIdAndUser(collectionId, user);
        if (existing == null) {
            throw new IllegalArgumentException ("Could not find the given collection " + collectionId + " for the user");
        }
        
        CollectionView cv = new CollectionView();
        cv.setCollectionId(existing.getCollectionId());
    	cv.setName(existing.getName());
    	cv.setDescription(existing.getDescription());
    	if (existing.getTags() != null) cv.setTags(new ArrayList<>(existing.getTags()));
    	cv.setChildren(new ArrayList<>());
    	if (existing.getCollections() != null) {
	    	for (Collection c: existing.getCollections()) {
	    		CollectionView child = new CollectionView();
	    		child.setCollectionId(c.getCollectionId());
	    		child.setName(c.getName());
	    		child.setDescription(c.getDescription());
	    		if (c.getMetadata() != null) child.setMetadata(new ArrayList<>(c.getMetadata()));
	    		if (c.getTags() != null) child.setTags(new ArrayList<>(c.getTags()));
	        	child.setGlycans(new ArrayList<Glycan>());
	        	for (GlycanInCollection gic: c.getGlycans()) {
	        		Glycan g = gic.getGlycan();
	        		g.setGlycanCollections(null);
	        		try {
	                    g.setCartoon(getImageForGlycan(g.getGlycanId()));
	                } catch (DataNotFoundException e) {
	                    // ignore
	                    logger.warn ("no image found for glycan " + g.getGlycanId());
	                }
	        		child.getGlycans().add(g);
	        	}
	        	cv.getChildren().add(child);
	    	}
    	}
        
        return new ResponseEntity<>(new SuccessResponse(cv, "collection retrieved"), HttpStatus.OK);
    }
    
    @Operation(summary = "Get batch uploads", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/checkbatchupload")
    @ApiResponses(value = { @ApiResponse(responseCode="200", description= "Check performed successfully", content = {
            @Content( schema = @Schema(implementation = SuccessResponse.class))}),
            @ApiResponse(responseCode="415", description= "Media type is not supported"),
            @ApiResponse(responseCode="500", description= "Internal Server Error") })
    public ResponseEntity<SuccessResponse> getActiveBatchUpload () {
    	// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        if (user != null) {
        	List<BatchUploadEntity> uploads = uploadRepository.findByUserOrderByStartDateDesc(user);	
        	if (uploads.isEmpty()) {
        		throw new DataNotFoundException("No active batch upload");
        	}
        	return new ResponseEntity<>(new SuccessResponse(uploads, "Batch uploads retrieved"), HttpStatus.OK);
        } else {
        	throw new BadRequestException("user cannot be found");
        }
    }
    
    @Operation(summary = "Mark batch upload process as read", 
            security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value = "/updatebatchupload/{uploadId}", method = RequestMethod.POST,
            produces={"application/json", "application/xml"})
    @ApiResponses(value = { @ApiResponse(responseCode="200", description= "Update performed successfully", content = {
            @Content( schema = @Schema(implementation = SuccessResponse.class))}),
            @ApiResponse(responseCode="415", description= "Media type is not supported"),
            @ApiResponse(responseCode="500", description= "Internal Server Error") })
    public ResponseEntity<SuccessResponse> updateActiveBatchUpload(
    		@Parameter(required=true, description="internal id of the file upload to mark as read") 
            @PathVariable("uploadId")
    		Long batchUploadId){

    	Optional<BatchUploadEntity> upload = uploadRepository.findById(batchUploadId);
    	if (upload != null) {
            BatchUploadEntity entity = upload.get();
            entity.setAccessedDate(new Date());
            uploadRepository.save(entity);
            return new ResponseEntity<>(new SuccessResponse(entity, "file upload is marked as read"), HttpStatus.OK);
        }
	
        throw new BadRequestException("file upload cannot be found");
    }
    
    @Operation(summary = "Delete the given file upload", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/deletefileupload/{uploadId}", method = RequestMethod.DELETE)
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Collection deleted successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to delete collections"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<SuccessResponse> deleteBatchUpload (
            @Parameter(required=true, description="id of the file upload to delete") 
            @PathVariable("uploadId") Long uploadId) {
        
        // get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        List<BatchUploadEntity> found = uploadRepository.findByUserAndId(user, uploadId);
        if (found.isEmpty()) {
        	throw new IllegalArgumentException("Could not find upload entry with the given id (" + uploadId + ") for this user");
        }
        uploadRepository.deleteById(uploadId);
        return new ResponseEntity<>(new SuccessResponse(uploadId, "Collection deleted successfully"), HttpStatus.OK);
    }
    
    @Operation(summary = "Send error report email", 
            security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value = "/senderrorreport/{errorId}", method = RequestMethod.POST,
            produces={"application/json", "application/xml"})
    @ApiResponses(value = { @ApiResponse(responseCode="200", description= "Email sent successfully", content = {
            @Content( schema = @Schema(implementation = SuccessResponse.class))}),
            @ApiResponse(responseCode="415", description= "Media type is not supported"),
            @ApiResponse(responseCode="500", description= "Internal Server Error") })
    public ResponseEntity<SuccessResponse> sendErrorReport(
    		@Parameter(required=true, description="internal id of the file upload") 
            @PathVariable("errorId")
    		Long errorId,
    		@Parameter(required=false, description="is report related to the upload")
    		@RequestParam (defaultValue="false")
    		Boolean isUpload) {
    	
    	// email error to admins
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
            logger.error("Cannot locate admin emails", e);
            throw new IllegalArgumentException("Error report not sent! Cannot locate admin emails");
        }
        
        if (isUpload) {
        	Optional<BatchUploadEntity> upload = uploadRepository.findById(errorId);
        	if (upload != null) {
        		emailManager.sendErrorReport(upload.get(), emails.toArray(new String[0]));
        		return new ResponseEntity<>(new SuccessResponse(upload, "file upload report is sent"), HttpStatus.OK);
        	}
        	
        } else {
	    	Optional<UploadErrorEntity> upload = uploadErrorRepository.findById(errorId);
	    	if (upload != null) {
	    		emailManager.sendErrorReport(upload.get(), emails.toArray(new String[0]));
	            return new ResponseEntity<>(new SuccessResponse(upload, "file upload report is sent"), HttpStatus.OK);
	        }
        }
	
        throw new BadRequestException("File upload error with the given id " + errorId + " cannot be found"); 
    }
    
    @Operation(summary = "Add tag for all glycans of the given file upload", 
            security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value = "/addtagforfileupload/{uploadId}", method = RequestMethod.POST,
            produces={"application/json", "application/xml"})
    @ApiResponses(value = { @ApiResponse(responseCode="200", description= "Update performed successfully", content = {
            @Content( schema = @Schema(implementation = SuccessResponse.class))}),
            @ApiResponse(responseCode="415", description= "Media type is not supported"),
            @ApiResponse(responseCode="500", description= "Internal Server Error") })
    public ResponseEntity<SuccessResponse> addTagForFileUpload(
    		@Parameter(required=true, description="internal id of the file upload") 
            @PathVariable("uploadId")
    		Long batchUploadId,
    		@RequestBody Object tag){
    	
    	if (tag == null || !(tag instanceof String) || ((String)tag).isEmpty()) {
    		throw new IllegalArgumentException("Tag cannnot be empty");
    	}
    	
    	// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }

    	Optional<BatchUploadEntity> upload = uploadRepository.findById(batchUploadId);
    	if (upload != null) {
            BatchUploadEntity entity = upload.get();
            if (entity.getGlycans() != null) {
            	List<Glycan> glycans = new ArrayList<>();
            	for (GlycanInFile g: entity.getGlycans()) {
            		glycans.add(g.getGlycan());
            	}
            	glycanManager.addTagToGlycans(glycans, (String)tag, user);
            }
            return new ResponseEntity<>(new SuccessResponse(entity, "the tag is added to all glycans of this file upload"), HttpStatus.OK);
        }
	
        throw new BadRequestException("file upload cannot be found");
    }
    
    @Operation(summary = "Add tag for the given glycan", 
            security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value = "/addglycantag/{glycanId}", method = RequestMethod.POST,
            produces={"application/json", "application/xml"})
    @ApiResponses(value = { @ApiResponse(responseCode="200", description= "Update performed successfully", content = {
            @Content( schema = @Schema(implementation = SuccessResponse.class))}),
            @ApiResponse(responseCode="415", description= "Media type is not supported"),
            @ApiResponse(responseCode="500", description= "Internal Server Error") })
    public ResponseEntity<SuccessResponse> updateGlycanTags(
    		@Parameter(required=true, description="internal id of the glycan") 
            @PathVariable("glycanId")
    		Long glycanId,
    		@RequestBody List<String> tags){
    	
    	if (tags == null) {
    		throw new IllegalArgumentException("Tags cannnot be null");
    	}
    	
    	// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }

    	Optional<Glycan> glycan = glycanRepository.findById(glycanId);
    	if (glycan != null) {
            Glycan g = glycan.get();
            glycanManager.setGlycanTags(g, tags, user);
            return new ResponseEntity<>(new SuccessResponse(g, "the given tags are added to the given glycan"), HttpStatus.OK);
        }
	
        throw new BadRequestException("glycan cannot be found");
    }
    

    @Operation(summary = "Get current glycan tags for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/getglycantags")
    public ResponseEntity<SuccessResponse> getGlycanTags() {
    	// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }

    	List<GlycanTag> tags = glycanManager.getTags(user);
    	return new ResponseEntity<>(new SuccessResponse(tags, "user's glycan tags retrieved successfully"), HttpStatus.OK);
    }
    
    @Operation(summary = "Delete given glycan from the user's list", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/deleteglycan/{glycanId}", method = RequestMethod.DELETE)
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Glycan deleted successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to delete glycans"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<SuccessResponse> deleteGlycan (
            @Parameter(required=true, description="internal id of the glycan to delete") 
            @PathVariable("glycanId") Long glycanId) {
        
        // get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        Glycan existing = glycanRepository.findByGlycanIdAndUser(glycanId, user);
        if (existing == null) {
            throw new IllegalArgumentException ("Could not find the given glycan " + glycanId + " for the user");
        }
        //need to check if the glycan appears in any collection and give an error message
        if (existing.getGlycanCollections() != null && !existing.getGlycanCollections().isEmpty()) {
        	String collectionString = "";
        	for (GlycanInCollection col: existing.getGlycanCollections()) {
        		collectionString += col.getCollection().getName() + ", ";
        	}
        	collectionString = collectionString.substring(0, collectionString.lastIndexOf(","));
        	throw new BadRequestException ("Cannot delete this glycan. It is used in the following collections: " + collectionString);
        }
        glycanRepository.deleteById(glycanId);
        return new ResponseEntity<>(new SuccessResponse(glycanId, "Glycan deleted successfully"), HttpStatus.OK);
    }
    
    @Operation(summary = "Delete the given collection from the user's list", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/deletecollection/{collectionId}", method = RequestMethod.DELETE)
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Collection deleted successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to delete collections"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<SuccessResponse> deleteCollection (
            @Parameter(required=true, description="id of the collection to delete") 
            @PathVariable("collectionId") Long collectionId) {
        
        // get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        Collection existing = collectionRepository.findByCollectionIdAndUser(collectionId, user);
        if (existing == null) {
            throw new IllegalArgumentException ("Could not find the given collection " + collectionId + " for the user");
        }
        //Check if there are any parent collections referencing this one
        List<Collection> parentList = new ArrayList<>();
        List<Collection> parentCollections = collectionRepository.findParentCollectionsByUser(user);
        for (Collection parent: parentCollections) {
        	for (Collection child: parent.getCollections()) {
        		if (child.getCollectionId().equals(collectionId)) {
        			parentList.add(parent);
        			break;
        		}
        	}
        }
        if (!parentList.isEmpty()) {
        	String parents = "";
        	for (Collection p: parentList) {
        		parents += p.getName() + ", ";
        	}
        	throw new BadRequestException("Cannot delete this collection since there are collections referencing it.\n"
        			+ "Delete from the parent collection first! Collection of collections referencing it: " + parents.substring(0, parents.lastIndexOf(", ")));
        }
        collectionRepository.deleteById(collectionId);
        return new ResponseEntity<>(new SuccessResponse(collectionId, "Collection deleted successfully"), HttpStatus.OK);
    }
    
    @Operation(summary = "Delete the given collection of collections from the user's list", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/deletecoc/{collectionId}", method = RequestMethod.DELETE)
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Collection deleted successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to delete collections"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<SuccessResponse> deleteCoC (
            @Parameter(required=true, description="id of the collection to delete") 
            @PathVariable("collectionId") Long collectionId) {
        
        // get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        Collection existing = collectionRepository.findByCollectionIdAndUser(collectionId, user);
        if (existing == null) {
            throw new IllegalArgumentException ("Could not find the given collection of collections (" + collectionId + ") for the user");
        }
        collectionRepository.deleteById(collectionId);
        return new ResponseEntity<>(new SuccessResponse(collectionId, "Collection deleted successfully"), HttpStatus.OK);
    }
    
    @Operation(summary = "Add glycan", security = { @SecurityRequirement(name = "bearer-key") })
    @PostMapping("/addglycan")
    public ResponseEntity<SuccessResponse> addGlycan(@Valid @RequestBody GlycanView g) {
        if ((g.getSequence() == null || g.getSequence().isEmpty()) 
                && (g.getGlytoucanID() == null || g.getGlytoucanID().isEmpty())
                && (g.getComposition() == null || g.getComposition().isEmpty())) {
            throw new IllegalArgumentException("Either the sequence or GlyToucan id or composition string must be provided to add glycans");
        }
        
        // get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        
        Glycan glycan = new Glycan();
        if (g.getGlytoucanID() != null && !g.getGlytoucanID().trim().isEmpty()) {
            glycan.setGlytoucanID(g.getGlytoucanID());
            // check for duplicates
            // error if there is already a glycan in the system
            Glycan existing = glycanRepository.findByGlytoucanIDIgnoreCaseAndUser(g.getGlytoucanID().trim(), user);
            if (existing != null) {
                throw new DuplicateException ("There is already a glycan with GlyTouCan ID " + g.getGlytoucanID(), null, existing);
            }
            // check glytoucan to see if the id is correct!
            String sequence = SequenceUtils.getSequenceFromGlytoucan(g.getGlytoucanID().trim());
            glycan.setWurcs(sequence);
            try {
                glycan.setMass(SequenceUtils.computeMassFromWurcs(glycan.getWurcs()));
            } catch (Exception e) {
                logger.error("could not calculate mass for wurcs sequence ", e);
                glycan.setError("Could not calculate mass. Reason: " + e.getMessage());
            }
            glycan.setStatus(RegistrationStatus.ALREADY_IN_GLYTOUCAN);
            
        } else if (g.getSequence() != null && !g.getSequence().trim().isEmpty()){ 
            parseAndRegisterGlycan(glycan, g, glycanRepository, user);
        } else { // composition
            try {
                Composition compo = CompositionUtils.parse(g.getComposition().trim());
                String strWURCS = CompositionConverter.toWURCS(compo);
                glycan.setWurcs(strWURCS);
                // recode the sequence
                WURCSValidator validator = new WURCSValidator();
                validator.start(glycan.getWurcs());
                if (validator.getReport().hasError()) {
                    String errorMessage = "";
                    for (String error: validator.getReport().getErrors()) {
                        errorMessage += error + ", ";
                    }
                    errorMessage = errorMessage.substring(0, errorMessage.lastIndexOf(","));
                    throw new IllegalArgumentException ("WURCS parse error. Details: " + errorMessage);
                } else {
                    glycan.setWurcs(validator.getReport().getStandardString());
                    try {
                    	glycan.setMass(SequenceUtils.computeMassFromWurcs(glycan.getWurcs()));
                    } catch (Exception e) {
                        logger.error("could not calculate mass for wurcs sequence ", e);
                        glycan.setError("Could not calculate mass. Reason: " + e.getMessage());
                    }
                }
                Glycan existing = glycanRepository.findByWurcsIgnoreCaseAndUser(glycan.getWurcs(), user);
                if (existing != null) {
                    throw new DuplicateException ("There is already a glycan with WURCS " + glycan.getWurcs(), null, existing);
                }
                SequenceUtils.getWurcsAndGlytoucanID(glycan, null);
                if (glycan.getGlytoucanID() == null || glycan.getGlytoucanID().isEmpty()) {
                	SequenceUtils.registerGlycan(glycan);
                }
            } catch (DictionaryException | CompositionParseException | ConversionException e1) {
                throw new IllegalArgumentException ("Composition parsing/conversion failed. Reason " + e1.getMessage());
            } catch (GlycoVisitorException e1) {
                throw new IllegalArgumentException (e1);
            } 
        }
        // save the glycan
        glycan.setDateCreated(new Date());
        glycan.setUser(user);
        Glycan added = glycanRepository.save(glycan);
        
        if (added != null) {
            BufferedImage t_image = createImageForGlycan(added);
            if (t_image != null) {
                String filename = added.getGlycanId() + ".png";
                //save the image into a file
                logger.debug("Adding image to " + imageLocation);
                File imageFile = new File(imageLocation + File.separator + filename);
                try {
                    ImageIO.write(t_image, "png", imageFile);
                } catch (IOException e) {
                    logger.error("could not write cartoon image to file", e);
                }
            } else {
                logger.warn ("Glycan image cannot be generated for glycan " + added.getGlycanId());
            }
        } 
        return new ResponseEntity<>(new SuccessResponse(glycan, "glycan added"), HttpStatus.OK);
    }
    
    @Operation(summary = "Add collection", security = { @SecurityRequirement(name = "bearer-key") })
    @PostMapping("/addcollection")
    public ResponseEntity<SuccessResponse> addCollection(@Valid @RequestBody CollectionView c) {
    	// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        
        // check if duplicate
    	
    	Collection collection = new Collection();
    	collection.setName(c.getName());
    	collection.setDescription(c.getDescription());
    	collection.setMetadata(c.getMetadata());
    	
    	if (c.getGlycans() != null && !c.getGlycans().isEmpty()) {
    		collection.setGlycans(new ArrayList<>());
    		for (Glycan g: c.getGlycans()) {
    			GlycanInCollection gic = new GlycanInCollection();
    			gic.setCollection(collection);
    			gic.setGlycan(g);
    			gic.setDateAdded(new Date());
    			collection.getGlycans().add(gic);
    		}
    	}
    	
        collection.setUser(user);
    	collectionRepository.save(collection);
    	return new ResponseEntity<>(new SuccessResponse(collection, "collection added"), HttpStatus.OK);
    }
    
    @Operation(summary = "Add collection of collections", security = { @SecurityRequirement(name = "bearer-key") })
    @PostMapping("/addcoc")
    public ResponseEntity<SuccessResponse> addCoC(@Valid @RequestBody CollectionView c) {
    	// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        
        if (c.getChildren() == null || c.getChildren().isEmpty()) {
        	throw new IllegalArgumentException("Collections cannot be empty!");
        }
        
        // check if duplicate
    	Collection collection = new Collection();
    	collection.setName(c.getName());
    	collection.setDescription(c.getDescription());
    	collection.setCollections(new ArrayList<>());
    	for (CollectionView child: c.getChildren()) {
    		Collection childEntity = collectionRepository.getReferenceById(child.getCollectionId());
    		childEntity.addParent(collection);
    		collection.getCollections().add(childEntity);
    	}
    	
        collection.setUser(user);
    	collectionRepository.save(collection);
    	return new ResponseEntity<>(new SuccessResponse(c, "collection added"), HttpStatus.OK);
    }
    
    @Operation(summary = "update collection", security = { @SecurityRequirement(name = "bearer-key") })
    @PostMapping("/updatecollection")
    public ResponseEntity<SuccessResponse> updateCollection(@Valid @RequestBody CollectionView c) {
    	if (c.getCollectionId() == null) {
    		throw new IllegalArgumentException("collection id should be provided for update");
    	}
    	// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
    	Collection existing = collectionRepository.findByCollectionIdAndUser(c.getCollectionId(), user);
    	if (existing == null) {
    		throw new IllegalArgumentException("Collection (" + c.getCollectionId() + ") to be updated cannot be found");
    	}
    	// check if name is a duplicate
    	if (!existing.getName().equalsIgnoreCase(c.getName())) {   // changing the name
	    	List<Collection> duplicate = collectionRepository.findAllByNameAndUser (c.getName(), user);
	    	if (!duplicate.isEmpty()) {
	    		throw new DuplicateException("Collection with name: " + c.getName() + " already exists! Pick a different name");
	    	}
    	}
    	existing.setName(c.getName());
    	existing.setDescription(c.getDescription());
    	if (existing.getGlycans() == null) {
    		existing.setGlycans(new ArrayList<>());
    	}
    	
    	if (c.getGlycans() == null || c.getGlycans().isEmpty()) {
    		existing.getGlycans().clear();
    	} else {
	    	// remove glycans as necessary
    		List<GlycanInCollection> toBeRemoved = new ArrayList<>();
	    	for (GlycanInCollection gic: existing.getGlycans()) {
	    		boolean found = false;
	    		for (Glycan g: c.getGlycans()) {
	    			if (g.getGlycanId().equals(gic.getGlycan().getGlycanId())) {
	    				// keep it
	    				found = true;
	    			}
	    		}
	    		if (!found) {
	    			toBeRemoved.add(gic);
	    		}
	    	}
	    	existing.getGlycans().removeAll(toBeRemoved);
    	}
    	
    	if (c.getGlycans() != null && !c.getGlycans().isEmpty()) {
    		// check if this glycan already exists in the collection
    		for (Glycan g: c.getGlycans()) {
    			boolean exists = false;
    			for (GlycanInCollection gic: existing.getGlycans()) {
        			if (gic.getGlycan().getGlycanId().equals(g.getGlycanId())) {
        				exists = true;
        				break;
        			}
        		}
    			if (!exists) {
	    			GlycanInCollection gic = new GlycanInCollection();
	    			gic.setCollection(existing);
	    			gic.setGlycan(g);
	    			gic.setDateAdded(new Date());
	    			existing.getGlycans().add(gic);
    			}
    		}
    	}
    	
    	existing.setMetadata(c.getMetadata());
    	collectionRepository.save(existing);
    	return new ResponseEntity<>(new SuccessResponse(c, "collection updated"), HttpStatus.OK);
    }
    
    @Operation(summary = "update collection of collections", security = { @SecurityRequirement(name = "bearer-key") })
    @PostMapping("/updatecoc")
    public ResponseEntity<SuccessResponse> updateCoC(@Valid @RequestBody CollectionView c) {
    	if (c.getCollectionId() == null) {
    		throw new IllegalArgumentException("collection id should be provided for update");
    	}
    	// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
    	Collection existing = collectionRepository.findByCollectionIdAndUser(c.getCollectionId(), user);
    	if (existing == null) {
    		throw new IllegalArgumentException("Collection of collections (" + c.getCollectionId() + ") to be updated cannot be found");
    	}
    	// check if name is a duplicate
    	if (!existing.getName().equalsIgnoreCase(c.getName())) {   // changing the name
	    	List<Collection> duplicate = collectionRepository.findAllByNameAndUser (c.getName(), user);
	    	if (!duplicate.isEmpty()) {
	    		throw new DuplicateException("Collection of collections with name: " + c.getName() + " already exists! Pick a different name");
	    	}
    	}
    	existing.setName(c.getName());
    	existing.setDescription(c.getDescription());
    	if (existing.getCollections() == null) {
    		existing.setCollections(new ArrayList<>());
    	}
    	
    	if (c.getChildren() == null || c.getChildren().isEmpty()) {
    		existing.getCollections().clear();
    	} else {
	    	// remove collections as necessary
    		List<Collection> toBeRemoved = new ArrayList<>();
	    	for (Collection child: existing.getCollections()) {
	    		boolean found = false;
	    		for (CollectionView col: c.getChildren()) {
	    			if (child.getCollectionId().equals(col.getCollectionId())) {
	    				// keep it
	    				found = true;
	    			}
	    		}
	    		if (!found) {
	    			toBeRemoved.add(child);
	    		}
	    	}
	    	existing.getCollections().removeAll(toBeRemoved);
    	}
    	
    	if (c.getChildren() != null && !c.getChildren().isEmpty()) {
    		// check if this collection already exists in the collection
    		for (CollectionView child: c.getChildren()) {
    			boolean exists = false;
    			for (Collection col: existing.getCollections()) {
        			if (col.getCollectionId().equals(child.getCollectionId())) {
        				exists = true;
        				break;
        			}
        		}
    			if (!exists) {
    				Collection collection = collectionRepository.getReferenceById(child.getCollectionId());
    				collection.addParent(existing);
	    			if (collection != null) existing.getCollections().add(collection);
    			}
    		}
    	}
    	
    	collectionRepository.save(existing);
    	return new ResponseEntity<>(new SuccessResponse(c, "collection of collections updated"), HttpStatus.OK);
    }
    
    @Operation(summary = "Add glycans from file", security = { @SecurityRequirement(name = "bearer-key") })
    @PostMapping("/addglycanfromfile")
    public ResponseEntity<SuccessResponse> addGlycansFromFile(
    		@Parameter(required=true, name="file", description="details of the uploded file") 
	        @RequestBody
    		FileWrapper fileWrapper, 
    		@Parameter(required=true, name="filetype", description="type of the file", schema = @Schema(type = "string",
    		allowableValues= {"GWS", "WURCS"})) 
	        @RequestParam(required=true, value="filetype") String fileType,
	        @RequestParam(required=false, value="tag") String tag) {
    	
    	SequenceFormat format = SequenceFormat.valueOf(fileType);
    	
    	// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        
        // check if there is an ongoing upload
    	List<BatchUploadEntity> batchList = uploadRepository.findByUser(user);
    	for (BatchUploadEntity b: batchList) {
    		if (b.getStatus() == UploadStatus.PROCESSING) {
    			throw new BadRequestException ("There is an ongoing glycan upload process. Please wait for that to finish before uploading a new file");
    		}
    	}
    	
	    
	    String fileFolder = uploadDir;
        if (fileWrapper.getFileFolder() != null && !fileWrapper.getFileFolder().isEmpty())
            fileFolder = fileWrapper.getFileFolder();
        File file = new File (fileFolder, fileWrapper.getIdentifier());
        if (!file.exists()) {
            throw new IllegalArgumentException("File is not acceptable");
        }
        else {
            byte[] fileContent;
            try {
                fileContent = Files.readAllBytes(file.toPath());
                BatchUploadEntity result = new BatchUploadEntity();
                result.setStartDate(new Date());
                result.setStatus(UploadStatus.PROCESSING);
                result.setUser(user);
                result.setFilename(fileWrapper.getOriginalName());
                result.setFormat(format.name());
                BatchUploadEntity saved = uploadRepository.save(result);
                // keep the original file in the uploads directory
                File uploadFolder = new File (uploadDir + File.separator + saved.getId());
                if (!uploadFolder.exists()) {
                	uploadFolder.mkdirs();
                }
                boolean success = file.renameTo(new File (uploadFolder + File.separator + fileWrapper.getOriginalName()));
                if (!success) {
                	logger.error("Could not store the original file");
                }
                try {    
                    CompletableFuture<SuccessResponse> response = null;
                    
                    // process the file and add the glycans 
                    switch (format) {
                    case GWS:
                    	response = batchUploadService.addGlycanFromTextFile(fileContent, saved, user, format, ";", tag);
                    	break;
                    case WURCS:
                    	response = batchUploadService.addGlycanFromTextFile(fileContent, saved, user, format, "\\n", tag);
                    	break;
					default:
						break;
                    }
                    
                    response.whenComplete((resp, e) -> {
                    	if (e != null) {
                            logger.error(e.getMessage(), e);
                            result.setStatus(UploadStatus.ERROR);
                            if (e.getCause() instanceof BatchUploadException) {
                            	result.setErrors(((BatchUploadException)e.getCause()).getErrors());
                    		} else if (result.getErrors() == null) {
                    			result.setErrors(new ArrayList<>());
                    			result.getErrors().add(new UploadErrorEntity(null, e.getCause().getMessage(), null));
                    		}
                            uploadRepository.save(result);
                            
                        } else {
                        	BatchUploadEntity upload = (BatchUploadEntity) resp.getData();
                        	int count = 0;
                        	for (GlycanInFile g: upload.getGlycans()) {
                        		if (!g.getIsNew()) {
                        			count++;
                        		}
                        	}
                        	result.setExistingCount(count);
                            result.setStatus(UploadStatus.DONE);    
                            result.setErrors(new ArrayList<>());
                            uploadRepository.save(result);
                        }                       
                    });
                    response.get(1000, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                	synchronized (this) {
                        if (result.getErrors() != null && !result.getErrors().isEmpty()) {
                            result.setStatus(UploadStatus.ERROR);
                        } 
                        uploadRepository.save(result);
                    }
                } catch (InterruptedException e1) {
					logger.error("batch upload is interrupted", e1);
				} catch (ExecutionException e1) {
					logger.error("batch upload is interrupted", e1);
				}
                 
            } catch (IOException e) {
                throw new IllegalArgumentException("File cannot be read. Reason: " + e.getMessage());
            }
    	    
        }
    	return new ResponseEntity<>(new SuccessResponse(null, "glycan added"), HttpStatus.OK); 
    }
    
    public static void parseAndRegisterGlycan (Glycan glycan, GlycanView g, GlycanRepository glycanRepository, UserEntity user) {
    	org.eurocarbdb.application.glycanbuilder.Glycan glycanObject= null;
        FixGlycoCtUtil fixGlycoCT = new FixGlycoCtUtil();
        Sugar sugar = null;
        try {
            switch (g.getFormat()) {
            case WURCS:
                glycan.setWurcs(g.getSequence().trim());
                // recode the sequence
                WURCSValidator validator = new WURCSValidator();
                validator.start(glycan.getWurcs());
                if (validator.getReport().hasError()) {
                    String errorMessage = "";
                    for (String error: validator.getReport().getErrors()) {
                        errorMessage += error + ", ";
                    }
                    errorMessage = errorMessage.substring(0, errorMessage.lastIndexOf(","));
                    throw new IllegalArgumentException ("WURCS parse error. Details: " + errorMessage);
                } else {
                    glycan.setWurcs(validator.getReport().getStandardString());
                    try {
                    	glycan.setMass(SequenceUtils.computeMassFromWurcs(glycan.getWurcs()));
                    } catch (Exception e) {
                        logger.error("could not calculate mass for wurcs sequence ", e);
                        glycan.setError("Could not calculate mass. Reason: " + e.getMessage());
                    }
                }
                
                Glycan existing = glycanRepository.findByWurcsIgnoreCaseAndUser(glycan.getWurcs(), user);
                if (existing != null) {
                    throw new DuplicateException ("There is already a glycan with WURCS " + glycan.getWurcs(), null, existing);
                }
                break;
            case GWS:
                glycan.setGws(g.getSequence().trim());
                existing = glycanRepository.findByGwsIgnoreCaseAndUser(glycan.getGws(), user);
                if (existing != null) {
                    throw new DuplicateException ("There is already a glycan with glycoworkbench sequence " + glycan.getGws(), null, existing);
                }
                try {
                    // parse and convert to GlycoCT
                    glycanObject = org.eurocarbdb.application.glycanbuilder.Glycan.fromString(glycan.getGws());
                    String glycoCT = glycanObject.toGlycoCTCondensed();
                    glycoCT = fixGlycoCT.fixGlycoCT(glycoCT);
                    glycan.setGlycoCT(glycoCT);
                    glycan.setMass(SequenceUtils.computeMass(glycanObject));
                } catch (Exception e) {
                    throw new IllegalArgumentException("GWS sequence is not valid. Reason: " + e.getMessage());
                }
                break;
            case GLYCOCT:
            default:
                glycan.setGlycoCT(g.getSequence().trim());
                // parse and convert to WURCS
                try {
                    glycanObject = org.eurocarbdb.application.glycanbuilder.Glycan.fromGlycoCTCondensed(glycan.getGlycoCT());
                    if (glycanObject != null) {
                        String glycoCT = glycanObject.toGlycoCTCondensed(); // required to fix formatting errors like extra line break etc.
                        glycoCT = fixGlycoCT.fixGlycoCT(glycoCT);
                        glycan.setGlycoCT(glycoCT);
                        glycan.setMass(SequenceUtils.computeMass(glycanObject));
                    }
                } catch (Exception e) {
                    logger.error("Glycan builder parse error", e.getMessage());
                    // check to make sure GlycoCT valid without using GWB
                    SugarImporterGlycoCTCondensed importer = new SugarImporterGlycoCTCondensed();
                    try {
                        sugar = importer.parse(glycan.getGlycoCT());
                        if (sugar == null) {
                            logger.error("Cannot get Sugar object for sequence:\n" + glycan.getGlycoCT());
                        } else {
                            SugarExporterGlycoCTCondensed exporter = new SugarExporterGlycoCTCondensed();
                            exporter.start(sugar);
                            String glycoCT = exporter.getHashCode();
                            glycoCT = fixGlycoCT.fixGlycoCT(glycoCT);
                            glycan.setGlycoCT(glycoCT);
                            // calculate mass
                            GlycoVisitorMass massVisitor = new GlycoVisitorMass();
                            massVisitor.start(sugar);
                            glycan.setMass(massVisitor.getMass(GlycoVisitorMass.DERIVATISATION_NONE));
                        }
                    } catch (Exception pe) {
                        logger.error("GlycoCT parsing failed", pe.getMessage());
                        throw new IllegalArgumentException ("GlycoCT parsing failed. Reason " + pe.getMessage());
                    }
                }
                existing = glycanRepository.findByGlycoCTIgnoreCaseAndUser(glycan.getGlycoCT(), user);
                if (existing != null) {
                    throw new DuplicateException ("There is already a glycan with GlycoCT " + glycan.getGlycoCT(), null, existing);
                }    
            }
            // check if the glycan has an accession number in Glytoucan
            SequenceUtils.getWurcsAndGlytoucanID(glycan, sugar);
        } catch (GlycoVisitorException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        
        if (glycan.getGlytoucanID() == null || glycan.getGlytoucanID().isEmpty()) {
        	SequenceUtils.registerGlycan(glycan);
        }   
    }
    
    public static BufferedImage createImageForGlycan(Glycan glycan) {
        BufferedImage t_image = null;
        org.eurocarbdb.application.glycanbuilder.Glycan glycanObject = null;
        try {
            if (glycan.getGlycoCT() != null && !glycan.getGlycoCT().isEmpty()) {
                glycanObject = 
                        org.eurocarbdb.application.glycanbuilder.Glycan.
                        fromGlycoCTCondensed(glycan.getGlycoCT().trim());
                if (glycanObject == null && glycan.getGlytoucanID() != null) {
                    String seq = GlytoucanUtil.getInstance().retrieveGlycan(glycan.getGlytoucanID());
                    if (seq != null) {
                        try {
                            WURCS2Parser t_wurcsparser = new WURCS2Parser();
                            glycanObject = t_wurcsparser.readGlycan(seq, new MassOptions());
                        } catch (Exception e) {
                            logger.error ("Glycan image cannot be generated with WURCS sequence", e);
                        }
                    }
                }
                
            } else if (glycan.getWurcs() != null && !glycan.getWurcs().isEmpty()) {
                WURCS2Parser t_wurcsparser = new WURCS2Parser();
                glycanObject = t_wurcsparser.readGlycan(glycan.getWurcs().trim(), new MassOptions());
            }
            if (glycanObject != null) {
                t_image = glycanWorkspace.getGlycanRenderer().getImage(glycanObject, true, false, true, 1.0D);
            } 

        } catch (Exception e) {
            logger.error ("Glycan image cannot be generated", e);
            // check if there is glytoucan id
            if (glycan.getGlytoucanID() != null) {
                String seq = GlytoucanUtil.getInstance().retrieveGlycan(glycan.getGlytoucanID());
                if (seq != null) {
                    WURCS2Parser t_wurcsparser = new WURCS2Parser();
                    try {
                        glycanObject = t_wurcsparser.readGlycan(seq, new MassOptions());
                        if (glycanObject != null) {
                            t_image = glycanWorkspace.getGlycanRenderer().getImage(glycanObject, true, false, true, 1.0D);
                        }
                    } catch (Exception e1) {
                        logger.error ("Glycan image cannot be generated from WURCS", e);
                    }
                }
            }
            
        }
        return t_image;
    }
    
    public byte[] getImageForGlycan (Long glycanId) {
        try {
            File imageFile = new File(imageLocation + File.separator + glycanId + ".png");
            InputStreamResource resource = new InputStreamResource(new FileInputStream(imageFile));
            return IOUtils.toByteArray(resource.getInputStream());
        } catch (IOException e) {
            logger.error("Image cannot be retrieved. Reason: " + e.getMessage());
            throw new DataNotFoundException("Image for glycan " + glycanId + " is not available");
        }
    }

}
