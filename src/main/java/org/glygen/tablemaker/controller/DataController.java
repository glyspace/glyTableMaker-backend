package org.glygen.tablemaker.controller;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.eurocarbdb.MolecularFramework.io.GlycoCT.SugarExporterGlycoCTCondensed;
import org.eurocarbdb.MolecularFramework.io.GlycoCT.SugarImporterGlycoCTCondensed;
import org.eurocarbdb.MolecularFramework.io.carbbank.SugarImporterCarbbank;
import org.eurocarbdb.MolecularFramework.io.cfg.SugarImporterCFG;
import org.eurocarbdb.MolecularFramework.io.namespace.GlycoVisitorToGlycoCT;
import org.eurocarbdb.MolecularFramework.sugar.Anomer;
import org.eurocarbdb.MolecularFramework.sugar.GlycoNode;
import org.eurocarbdb.MolecularFramework.sugar.GlycoconjugateException;
import org.eurocarbdb.MolecularFramework.sugar.Monosaccharide;
import org.eurocarbdb.MolecularFramework.sugar.Sugar;
import org.eurocarbdb.MolecularFramework.util.analytical.mass.GlycoVisitorMass;
import org.eurocarbdb.MolecularFramework.util.visitor.GlycoVisitorException;
import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.massutil.IonCloud;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.eurocarbdb.application.glycanbuilder.util.GraphicOptions;
import org.eurocarbdb.resourcesdb.Config;
import org.eurocarbdb.resourcesdb.GlycanNamescheme;
import org.eurocarbdb.resourcesdb.io.MonosaccharideConverter;
import org.glycoinfo.GlycanCompositionConverter.conversion.CompositionConverter;
import org.glycoinfo.GlycanCompositionConverter.conversion.ConversionException;
import org.glycoinfo.GlycanCompositionConverter.structure.Composition;
import org.glycoinfo.GlycanCompositionConverter.utils.CompositionParseException;
import org.glycoinfo.GlycanCompositionConverter.utils.CompositionUtils;
import org.glycoinfo.GlycanCompositionConverter.utils.DictionaryException;
import org.glycoinfo.GlycanFormatconverter.util.exchange.SugarToWURCSGraph.SugarToWURCSGraph;
import org.glycoinfo.WURCSFramework.util.WURCSException;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;
import org.glycoinfo.WURCSFramework.util.exchange.WURCSExchangeException;
import org.glycoinfo.WURCSFramework.util.subsumption.WURCSSubsumptionConverter;
import org.glycoinfo.WURCSFramework.util.validation.WURCSValidator;
import org.glycoinfo.WURCSFramework.wurcs.array.LIN;
import org.glycoinfo.WURCSFramework.wurcs.array.MS;
import org.glycoinfo.WURCSFramework.wurcs.array.RES;
import org.glycoinfo.WURCSFramework.wurcs.array.UniqueRES;
import org.glycoinfo.WURCSFramework.wurcs.array.WURCSArray;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.glygen.tablemaker.exception.BadRequestException;
import org.glygen.tablemaker.exception.BatchUploadException;
import org.glygen.tablemaker.exception.DataNotFoundException;
import org.glygen.tablemaker.exception.DuplicateException;
import org.glygen.tablemaker.exception.GlytoucanAPIFailedException;
import org.glygen.tablemaker.exception.GlytoucanFailedException;
import org.glygen.tablemaker.persistence.BatchUploadEntity;
import org.glygen.tablemaker.persistence.BatchUploadJob;
import org.glygen.tablemaker.persistence.ErrorReportEntity;
import org.glygen.tablemaker.persistence.GlycanImageEntity;
import org.glygen.tablemaker.persistence.UploadErrorEntity;
import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.UserError;
import org.glygen.tablemaker.persistence.dao.BatchUploadJobRepository;
import org.glygen.tablemaker.persistence.dao.BatchUploadRepository;
import org.glygen.tablemaker.persistence.dao.CollectionRepository;
import org.glygen.tablemaker.persistence.dao.CollectionSpecification;
import org.glygen.tablemaker.persistence.dao.CollectionTagRepository;
import org.glygen.tablemaker.persistence.dao.DatasetRepository;
import org.glygen.tablemaker.persistence.dao.DatasetSpecification;
import org.glygen.tablemaker.persistence.dao.GlycanImageRepository;
import org.glygen.tablemaker.persistence.dao.GlycanRepository;
import org.glygen.tablemaker.persistence.dao.GlycanSpecifications;
import org.glygen.tablemaker.persistence.dao.GlycanTagRepository;
import org.glygen.tablemaker.persistence.dao.GlycoproteinRepository;
import org.glygen.tablemaker.persistence.dao.GlycoproteinSpecification;
import org.glygen.tablemaker.persistence.dao.NamespaceRepository;
import org.glygen.tablemaker.persistence.dao.TableReportRepository;
import org.glygen.tablemaker.persistence.dao.UploadErrorRepository;
import org.glygen.tablemaker.persistence.dao.UserRepository;
import org.glygen.tablemaker.persistence.glycan.Collection;
import org.glygen.tablemaker.persistence.glycan.CollectionTag;
import org.glygen.tablemaker.persistence.glycan.CollectionType;
import org.glygen.tablemaker.persistence.glycan.CompositionType;
import org.glygen.tablemaker.persistence.glycan.Glycan;
import org.glygen.tablemaker.persistence.glycan.GlycanFileFormat;
import org.glygen.tablemaker.persistence.glycan.GlycanInCollection;
import org.glygen.tablemaker.persistence.glycan.GlycanInFile;
import org.glygen.tablemaker.persistence.glycan.GlycanTag;
import org.glygen.tablemaker.persistence.glycan.Metadata;
import org.glygen.tablemaker.persistence.glycan.RegistrationStatus;
import org.glygen.tablemaker.persistence.glycan.UploadStatus;
import org.glygen.tablemaker.persistence.protein.GlycanInSite;
import org.glygen.tablemaker.persistence.protein.Glycoprotein;
import org.glygen.tablemaker.persistence.protein.GlycoproteinInCollection;
import org.glygen.tablemaker.persistence.protein.GlycoproteinInFile;
import org.glygen.tablemaker.persistence.protein.GlycoproteinSiteType;
import org.glygen.tablemaker.persistence.protein.MultipleGlycanOrder;
import org.glygen.tablemaker.persistence.protein.Position;
import org.glygen.tablemaker.persistence.protein.Site;
import org.glygen.tablemaker.persistence.protein.SitePosition;
import org.glygen.tablemaker.persistence.table.TableReport;
import org.glygen.tablemaker.persistence.table.TableReportDetail;
import org.glygen.tablemaker.service.AsyncService;
import org.glygen.tablemaker.service.CollectionManager;
import org.glygen.tablemaker.service.EmailManager;
import org.glygen.tablemaker.service.ErrorReportingService;
import org.glygen.tablemaker.service.GlycanManagerImpl;
import org.glygen.tablemaker.service.ScheduledTasksService;
import org.glygen.tablemaker.util.FixGlycoCtUtil;
import org.glygen.tablemaker.util.GlytoucanUtil;
import org.glygen.tablemaker.util.SequenceUtils;
import org.glygen.tablemaker.view.CollectionView;
import org.glygen.tablemaker.view.FileWrapper;
import org.glygen.tablemaker.view.Filter;
import org.glygen.tablemaker.view.GlycanInSiteView;
import org.glygen.tablemaker.view.GlycanView;
import org.glygen.tablemaker.view.GlycoproteinView;
import org.glygen.tablemaker.view.SequenceFormat;
import org.glygen.tablemaker.view.SiteView;
import org.glygen.tablemaker.view.Sorting;
import org.glygen.tablemaker.view.SuccessResponse;
import org.glygen.tablemaker.view.UserStatisticsView;
import org.glygen.tablemaker.view.dto.CollectionDTO;
import org.glygen.tablemaker.view.dto.GlycanDTO;
import org.glygen.tablemaker.view.dto.GlycanInSiteDTO;
import org.glygen.tablemaker.view.dto.GlycoproteinDTO;
import org.glygen.tablemaker.view.dto.SiteDTO;
import org.grits.toolbox.glycanarray.om.parser.cfg.CFGMasterListParser;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import jakarta.persistence.EntityNotFoundException;
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
    final private GlycanTagRepository glycanTagRepository;
    final private CollectionRepository collectionRepository;
    final private CollectionTagRepository collectionTagRepository;
    final private UserRepository userRepository;
    final private BatchUploadRepository uploadRepository;
    final private AsyncService batchUploadService;
    final private GlycanManagerImpl glycanManager;
    final private UploadErrorRepository uploadErrorRepository;
    final private EmailManager emailManager;
    final private CollectionManager collectionManager;
    final private TableReportRepository reportRepository;
    final private NamespaceRepository namespaceRepository;
    final private GlycanImageRepository glycanImageRepository;
    final private DatasetRepository datasetRepository;
    final private GlycoproteinRepository glycoproteinRepository;
    final private BatchUploadJobRepository batchUploadJobRepository;
    final private ErrorReportingService errorReportingService;
    
    @Value("${spring.file.imagedirectory}")
    String imageLocation;
    
    @Value("${spring.file.uploaddirectory}")
	String uploadDir;
    
    public DataController(GlycanRepository glycanRepository, UserRepository userRepository,
    		BatchUploadRepository uploadRepository, AsyncService uploadService, 
    		CollectionRepository collectionRepository, GlycanManagerImpl glycanManager, 
    		UploadErrorRepository uploadErrorRepository, EmailManager emailManager, CollectionManager collectionManager, 
    		TableReportRepository reportRepository, NamespaceRepository namespaceRepository, 
    		GlycanImageRepository glycanImageRepository, DatasetRepository datasetRepository, 
    		GlycoproteinRepository glycoproteinRepository, BatchUploadJobRepository batchUploadJobRepository, ErrorReportingService errorReportingService, GlycanTagRepository glycanTagRepository, CollectionTagRepository collectionTagRepository) {
        this.glycanRepository = glycanRepository;
		this.glycanTagRepository = glycanTagRepository;
		this.collectionRepository = collectionRepository;
		this.collectionTagRepository = collectionTagRepository;
        this.userRepository = userRepository;
        this.uploadRepository = uploadRepository;
		this.batchUploadService = uploadService;
		this.glycanManager = glycanManager;
		this.uploadErrorRepository = uploadErrorRepository;
		this.emailManager = emailManager;
		this.collectionManager = collectionManager;
		this.reportRepository = reportRepository;
		this.namespaceRepository = namespaceRepository;
		this.glycanImageRepository = glycanImageRepository;
		this.datasetRepository = datasetRepository;
		this.glycoproteinRepository = glycoproteinRepository;
		this.batchUploadJobRepository = batchUploadJobRepository;
		this.errorReportingService = errorReportingService;
    }
    
    @Operation(summary = "Get data counts", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/statistics")
    public ResponseEntity<SuccessResponse<UserStatisticsView>> getStatistics() {
        UserStatisticsView stats = new UserStatisticsView();
     // get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        stats.setGlycanCount(glycanRepository.count(GlycanSpecifications.hasUserWithId(user.getUserId())));
        stats.setDatasetCount(datasetRepository.count(DatasetSpecification.hasUserWithId(user.getUserId())));
        Specification<Collection> spec = CollectionSpecification.hasUserWithId(user.getUserId());
    	spec = Specification.where(spec).and(CollectionSpecification.hasNoChildren());
        stats.setCollectionCount(collectionRepository.count(spec));
        spec = CollectionSpecification.hasUserWithId(user.getUserId());
    	spec = Specification.where(spec).and(CollectionSpecification.hasChildren());
    	stats.setCocCount(collectionRepository.count(spec));
    	stats.setGlycoproteinCount(glycoproteinRepository.count(GlycoproteinSpecification.hasUserWithId(user.getUserId())));
        return new ResponseEntity<>(new SuccessResponse<UserStatisticsView>(stats, "statistics gathered"), HttpStatus.OK);
    }
    
    @Operation(summary = "Get glycans", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/getglycans")
    public ResponseEntity<SuccessResponse> getGlycans(
            @RequestParam("start")
            Integer start, 
            @RequestParam(required=false, value="size")
            Integer size,
            @RequestParam(required=false, value="filters")
            String filters,
            @RequestParam(required=false, value="globalFilter")
            String globalFilter,
            @RequestParam(required=false, value="sorting")
            String sorting) {
        // get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
      
        if (size == null) {   // get all
        	size = Integer.MAX_VALUE;
        }
        // parse filters and sorting
        ObjectMapper mapper = new ObjectMapper();
        List<Filter> filterList = null;
        if (filters != null && !filters.equals("[]")) {
        	filters = URLDecoder.decode(filters, StandardCharsets.UTF_8);
            try {
                filterList = mapper.readValue(filters, 
                    new TypeReference<ArrayList<Filter>>() {});
            } catch (JsonProcessingException e) {
                throw new InternalError("filter parameter is invalid " + filters, e);
            }
        }
        
        List<Sorting> sortingList = null;
        List<Order> sortOrders = new ArrayList<>();
        boolean orderByTags = false;
        
        if (sorting != null && !sorting.equals("[]")) {
            try {
                sortingList = mapper.readValue(sorting, 
                    new TypeReference<ArrayList<Sorting>>() {});
                for (Sorting s: sortingList) {
                	if (s.getId().equalsIgnoreCase("tags")) {
                		orderByTags = true;
                		sortOrders.add(new Order(s.getDesc() ? Direction.DESC: Direction.ASC, s.getId()).nullsFirst());
                	} else {
                		sortOrders.add(new Order(s.getDesc() ? Direction.DESC: Direction.ASC, s.getId()).nullsFirst());
                	}
                }
            } catch (JsonProcessingException e) {
                throw new InternalError("sorting parameter is invalid " + sorting, e);
            }
        }
        
        Page<Glycan> glycansInPage = null;
       
        List<Glycan> pageGlycans = null;
        int currentPage = 0;
        long totalItems = 0;
        int totalPages = 0;
        
    	try {
    		boolean orFilter = false;
			String glytoucanId = null;
			String mass = null;
			String tagValue = null;
			if (globalFilter != null && !globalFilter.isBlank() && !globalFilter.equalsIgnoreCase("undefined")) {
				glytoucanId = globalFilter;
				mass = globalFilter;
				tagValue = globalFilter;
				orFilter = true;
			} else if (filterList != null) {
				for (Filter f: filterList) {
					if (f.getId().equalsIgnoreCase("mass")) {
						mass = f.getValue();
					}
					if (f.getId().equalsIgnoreCase("glytoucanId")) {
						glytoucanId = f.getValue();
					}
					if (f.getId().equalsIgnoreCase("tags")) {
						tagValue = f.getValue();
					}
				}
			}
			glycansInPage = glycanRepository.searchGlycans(tagValue, glytoucanId, mass, user, orFilter, orderByTags, PageRequest.of(start, size, Sort.by(sortOrders)));
			pageGlycans = glycansInPage.getContent();
			currentPage = glycansInPage.getNumber();
			totalItems = glycansInPage.getTotalElements();
			totalPages = glycansInPage.getNumberOfElements();
    	} catch (Exception e) {
    		logger.error(e.getMessage(), e);
    		throw e;
    	}
        
        // retrieve cartoon images
        for (Glycan g: pageGlycans) {
        	// generate optional byonic and condensed composition strings
        	SequenceUtils.addCompositionInformation(g);
        	//g.setByonicString(SequenceUtils.generateByonicString(g));
        	//g.setCondensedString(SequenceUtils.generateCondensedString(g));
        	Optional<GlycanImageEntity> imageHandle = glycanImageRepository.findByGlycanId(g.getGlycanId());
        	if (!imageHandle.isPresent()) {
        		// create entry
        		GlycanImageEntity entity = new GlycanImageEntity();
        		entity.setGlycanId(g.getGlycanId());
        		entity.setGlytoucanId(g.getGlytoucanID());
        		entity.setWurcs(g.getWurcs());
        		glycanImageRepository.save(entity);
        	}
            try {
                g.setCartoon(getImageForGlycan(imageLocation, g.getGlycanId()));
                if (g.getGlytoucanID() == null && g.getGlytoucanHash() != null && g.getWurcs() != null) {
                	try {
                		// registered, try to get the accession number
                		g.setGlytoucanID(GlytoucanUtil.getInstance().getAccessionNumber(g.getWurcs()));
                		if (g.getGlytoucanID() == null) {
                			String accession = GlytoucanUtil.getInstance().checkBatchStatus(g.getGlytoucanHash());
                			if (accession != null && accession.startsWith("G")) {
                				g.setGlytoucanID(accession);
                			}
                		}
                		if (g.getGlytoucanID() != null) {
                			//update glycan's status to NEWLY_REGISTERED
                			g.setStatus(RegistrationStatus.NEWLY_REGISTERED);
                			// update glycan image table
                			imageHandle = glycanImageRepository.findByGlycanId(g.getGlycanId());
                			if (imageHandle.isPresent()) {
                				GlycanImageEntity entity = imageHandle.get();
                				entity.setGlytoucanId(g.getGlytoucanID());
                				glycanImageRepository.save(entity);
                			}
                		}
                	} catch (GlytoucanFailedException e) {
                		g.setError("Error registering. No additional information received from GlyTouCan.");
                		g.setStatus(RegistrationStatus.ERROR);
                		g.setErrorJson(e.getErrorJson());
                	} catch (GlytoucanAPIFailedException e) {
                		// API failure
                		logger.error(e.getMessage());
                	}
                	// save glycan with the updated information
                	glycanRepository.save(g);
                } else {
                	if (g.getGlytoucanID() != null) {
                		List<GlycanImageEntity> images = glycanImageRepository.findByGlytoucanId(g.getGlytoucanID());
            			if (images == null || images.isEmpty()) {
            				// update glycan image table
                			imageHandle = glycanImageRepository.findByGlycanId(g.getGlycanId());
                			if (imageHandle.isPresent()) {
                				GlycanImageEntity entity = imageHandle.get();
                				entity.setGlytoucanId(g.getGlytoucanID());
                				glycanImageRepository.save(entity);
                			}
                			else {
                				logger.error("Cannot find glycan with id " + g.getGlycanId() + " in the image repository");
                			}
            			}
                	}
                }
            } catch (DataNotFoundException e) {
                // ignore
                logger.warn ("no image found for glycan " + g.getGlycanId());
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("objects", pageGlycans);
        response.put("currentPage", currentPage);
        response.put("totalItems", totalItems);
        response.put("totalPages", totalPages);
        
        return new ResponseEntity<>(new SuccessResponse(response, "glycans retrieved"), HttpStatus.OK);
    }

	@Operation(summary = "Get user's glycoproteins", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/getglycoproteins")
    public ResponseEntity<SuccessResponse> getGlycoproteins(
            @RequestParam("start")
            Integer start, 
            @RequestParam(required=false, value="size")
            Integer size,
            @RequestParam(required=false, value="filters")
            String filters,
            @RequestParam(required=false, value="globalFilter")
            String globalFilter,
            @RequestParam(required=false, value="sorting")
            String sorting) {
        // get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        
        if (size == null) {   // get all
        	size = Integer.MAX_VALUE;
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
        boolean orderByTags = false;
        boolean orderBySites = false;
        if (sorting != null && !sorting.equals("[]")) {
            try {
                sortingList = mapper.readValue(sorting, 
                    new TypeReference<ArrayList<Sorting>>() {});
                for (Sorting s: sortingList) {
                	if (s.getId().equalsIgnoreCase("tags")) {
                		orderByTags = true;
                		sortOrders.add(new Order(s.getDesc() ? Direction.DESC: Direction.ASC, s.getId()).nullsFirst());
                	} else if (s.getId().equalsIgnoreCase("siteNo")) {
                		orderBySites = true;
                		sortOrders.add(new Order(s.getDesc() ? Direction.DESC: Direction.ASC, s.getId()).nullsFirst());
                	} else {
                		sortOrders.add(new Order(s.getDesc() ? Direction.DESC: Direction.ASC, s.getId()).nullsFirst());
                	}
                }
            } catch (JsonProcessingException e) {
                throw new InternalError("sorting parameter is invalid " + sorting, e);
            }
        }
        
        Page<Glycoprotein> glycoproteinsInPage = null;
    	try {
    		boolean orFilter = false;
			String uniprotId = null;
			String siteNo = null;
			String tagValue = null;
			String proteinName = null;
			String name = null;
			String seqVersion = null;
			if (globalFilter != null && !globalFilter.isBlank() && !globalFilter.equalsIgnoreCase("undefined")) {
				uniprotId = globalFilter;
				siteNo = globalFilter;
				tagValue = globalFilter;
				proteinName = globalFilter;
				name = globalFilter;
				seqVersion = globalFilter;
				orFilter = true;
			} else if (filterList != null) {
				for (Filter f: filterList) {
					if (f.getId().equalsIgnoreCase("siteNo")) {
						siteNo = f.getValue();
					}
					if (f.getId().equalsIgnoreCase("uniprotId")) {
						uniprotId = f.getValue();
					}
					if (f.getId().equalsIgnoreCase("tags")) {
						tagValue = f.getValue();
					}
					if (f.getId().equalsIgnoreCase("proteinName")) {
						proteinName = f.getValue();
					}
					if (f.getId().equalsIgnoreCase("name")) {
						name = f.getValue();
					}
					if (f.getId().equalsIgnoreCase("sequenceVersion")) {
						seqVersion = f.getValue();
					}
				}
			}
    		glycoproteinsInPage = glycoproteinRepository.searchGlycoproteins(tagValue, uniprotId, name, proteinName, seqVersion, siteNo, user, 
    				orFilter, orderByTags, orderBySites, PageRequest.of(start, size, Sort.by(sortOrders)));
    	} catch (Exception e) {
    		logger.error(e.getMessage(), e);
    		throw e;
    	}
        
        List<GlycoproteinView> glycoproteins = new ArrayList<>();
        for (Glycoprotein p: glycoproteinsInPage.getContent()) {
        	boolean updated = false;
        	if (p.getSites() != null) {
        		for (Site s: p.getSites()) {
        			if (s.getPositionString() != null) {
        				ObjectMapper om = new ObjectMapper();
        				try {
							s.setPosition(om.readValue(s.getPositionString(), SitePosition.class));
							// if aminoacid is missing, update it and save it again
							if (s.getPosition().getPositionList() != null) {
								for (Position pos: s.getPosition().getPositionList()) {
									if (pos.getLocation() != null && (pos.getAminoAcid() == null || pos.getAminoAcid().isEmpty())) {
										if (pos.getLocation().intValue() > 0) {
											pos.setAminoAcid (p.getSequence().charAt(pos.getLocation().intValue()-1) + "");
											updated = true;
										}
									}
								}
							}
							if (updated) {
								s.setPositionString(s.getPosition().toString());
							}
						} catch (JsonProcessingException e) {
							logger.warn ("Position string is invalid: " + s.getPositionString());
						}
        			}
        			
    				for (GlycanInSite gic: s.getGlycans()) {
    	        		Glycan g = gic.getGlycan();
    	        		if (g != null) {
	    	        		g.setGlycanCollections(null);
	    	        		g.setSites(null);
	    	        		SequenceUtils.addCompositionInformation(g);
	    	        		try {
	    	                    g.setCartoon(getImageForGlycan(imageLocation, g.getGlycanId()));
	    	                } catch (DataNotFoundException e) {
	    	                    // ignore
	    	                    logger.warn ("no image found for glycan " + g.getGlycanId());
	    	                }
    	        		}
    	        	}
        		}
        	}
        	if (updated) {
        		glycoproteinRepository.save(p);
        	}
        	GlycoproteinView view = new GlycoproteinView (p);
        	glycoproteins.add(view);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("objects", glycoproteins);
        response.put("currentPage", glycoproteinsInPage.getNumber());
        response.put("totalItems", glycoproteinsInPage.getTotalElements());
        response.put("totalPages", glycoproteinsInPage.getTotalPages());
        
        return new ResponseEntity<>(new SuccessResponse(response, "glycoproteins retrieved"), HttpStatus.OK);
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
      
        Map<String, Object> response = getCollections(user, collectionRepository, imageLocation, start, size, filters, globalFilter, sorting);
        return new ResponseEntity<>(new SuccessResponse(response, "collections retrieved"), HttpStatus.OK);
    }
    
    public static Map<String, Object> getCollections (
    		UserEntity user, CollectionRepository collectionRepository, String imageLocation,
    		Integer start, Integer size, String filters, String globalFilter, String sorting) {
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
        boolean sortByGlycanNo = false;
        boolean sortByGlycoproteinNo = false;
        boolean sortByMetadataNo = false;
        boolean asc = false;
        if (sorting != null && !sorting.equals("[]")) {
            try {
                sortingList = mapper.readValue(sorting, 
                    new TypeReference<ArrayList<Sorting>>() {});
                for (Sorting s: sortingList) {
                	if (s.getId().equalsIgnoreCase("glycanNo")) {
                		sortByGlycanNo = true;
                		asc = s.getDesc() ? false : true;
                	} else if (s.getId().equalsIgnoreCase("proteinNo")) {
                		sortByGlycoproteinNo = true;
                		asc = s.getDesc() ? false : true;
                	} else if (s.getId().equalsIgnoreCase("metadata")) {
                		sortByMetadataNo = true;
                		asc = s.getDesc() ? false : true;
                	} else {
                		sortOrders.add(new Order(s.getDesc() ? Direction.DESC: Direction.ASC, s.getId()));
                	}
                }
            } catch (JsonProcessingException e) {
                throw new InternalError("sorting parameter is invalid " + sorting, e);
            }
        }
        
        // apply filters
        List<Specification<Collection>> specificationList = new ArrayList<>();
        List<Specification<Collection>> globalSpecificationList = new ArrayList<>();
        
        if (filterList != null) {
	        for (Filter f: filterList) {
	        	CollectionSpecification spec = new CollectionSpecification(f);
	        	specificationList.add(spec);
	        }
        }
        
        if (globalFilter != null && !globalFilter.isBlank() && !globalFilter.equalsIgnoreCase("undefined")) {
        	globalSpecificationList.add(new CollectionSpecification(new Filter ("name", globalFilter)));
        	globalSpecificationList.add(new CollectionSpecification(new Filter ("description", globalFilter)));
        }
        
        Specification<Collection> globalSpec = null;
        if (!globalSpecificationList.isEmpty()) {
        	globalSpec = globalSpecificationList.get(0);
        	for (int i=1; i < globalSpecificationList.size(); i++) {
        		globalSpec = Specification.where(globalSpec).or(globalSpecificationList.get(i)); 
        	}
        }
        
        
        Specification<Collection> spec = null;
        if (globalSpec != null && specificationList.isEmpty()) { // no more filters, add the user filter
        	spec = Specification.where(globalSpec).and(CollectionSpecification.hasUserWithId(user.getUserId()));
        	if (sortByGlycanNo) spec = Specification.where (spec).and(CollectionSpecification.orderBySize(asc, "glycans"));
        	if (sortByGlycoproteinNo) spec = Specification.where (spec).and(CollectionSpecification.orderBySize(asc, "glycoproteins"));
        	if (sortByMetadataNo) spec = Specification.where (spec).and(CollectionSpecification.orderBySize(asc, "metadata"));
        } else {
	        if (!specificationList.isEmpty()) {
	        	spec = specificationList.get(0);
	        	for (int i=1; i < specificationList.size(); i++) {
	        		spec = Specification.where(spec).and(specificationList.get(i)); 
	        	}
	        	
	        	spec = Specification.where(spec).and(CollectionSpecification.hasUserWithId(user.getUserId()));
	        	spec = Specification.where(spec).and(CollectionSpecification.hasNoChildren());
	        	
	        	if (globalSpec != null) {
	        		spec = Specification.where(spec).and(globalSpec);
	        	}
	        }
	        if (sortByGlycanNo || sortByGlycoproteinNo || sortByMetadataNo) {
	        	if (specificationList.isEmpty()) {
	        		spec = Specification.where(spec).and(CollectionSpecification.hasUserWithId(user.getUserId()));
	        		spec = Specification.where(spec).and(CollectionSpecification.hasNoChildren());
	        	}
	        	spec = Specification.where (spec).and(
	        			CollectionSpecification.orderBySize(asc, 
	        					sortByGlycanNo ? "glycans" : sortByGlycoproteinNo ? "glycoproteins" : "metadata")); 
	        				
	        }
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
        	CollectionView cv = createCollectionView (c, imageLocation);
        	collections.add(cv);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("objects", collections);
        response.put("currentPage", collectionsInPage.getNumber());
        response.put("totalItems", collectionsInPage.getTotalElements());
        response.put("totalPages", collectionsInPage.getTotalPages());
        
        return response;
    }
    
    
    
    @Operation(summary = "Get collections of collections", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/getcocs")
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
        
        Map<String, Object> response = getCollectionsoOfCollections(user, collectionRepository, imageLocation, start, size, filters, globalFilter, sorting);
        
        return new ResponseEntity<>(new SuccessResponse(response, "collections of collections retrieved"), HttpStatus.OK);
    }
    
    public static Map<String, Object> getCollectionsoOfCollections (
    		UserEntity user, CollectionRepository collectionRepository, String imageLocation,
    		Integer start, Integer size, String filters, String globalFilter, String sorting) {
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
        boolean sortByChildren = false;
        boolean asc = false;
        if (sorting != null && !sorting.equals("[]")) {
            try {
                sortingList = mapper.readValue(sorting, 
                    new TypeReference<ArrayList<Sorting>>() {});
                for (Sorting s: sortingList) {
                	if (s.getId().equalsIgnoreCase("children")) {
                		sortByChildren = true;
                		asc = s.getDesc() ? false : true;
                	} else {
                		sortOrders.add(new Order(s.getDesc() ? Direction.DESC: Direction.ASC, s.getId()));
                	}
                }
            } catch (JsonProcessingException e) {
                throw new InternalError("sorting parameter is invalid " + sorting, e);
            }
        }
        
     // apply filters
        List<Specification<Collection>> specificationList = new ArrayList<>();
        List<Specification<Collection>> globalSpecificationList = new ArrayList<>();
        
        if (filterList != null) {
	        for (Filter f: filterList) {
	        	CollectionSpecification spec = new CollectionSpecification(f);
	        	specificationList.add(spec);
	        }
        }
        
        if (globalFilter != null && !globalFilter.isBlank() && !globalFilter.equalsIgnoreCase("undefined")) {
        	globalSpecificationList.add(new CollectionSpecification(new Filter ("name", globalFilter)));
        	globalSpecificationList.add(new CollectionSpecification(new Filter ("description", globalFilter)));
        }
        
        Specification<Collection> globalSpec = null;
        if (!globalSpecificationList.isEmpty()) {
        	globalSpec = globalSpecificationList.get(0);
        	for (int i=1; i < globalSpecificationList.size(); i++) {
        		globalSpec = Specification.where(globalSpec).or(globalSpecificationList.get(i)); 
        	}
        }
        
        Specification<Collection> spec = null;
        if (globalSpec != null && specificationList.isEmpty()) { // no more filters, add the user filter
        	spec = Specification.where(globalSpec).and(CollectionSpecification.hasUserWithId(user.getUserId()));
        	if (sortByChildren) spec = Specification.where (spec).and(CollectionSpecification.orderBySize(asc, "collections"));
        	
        } else {
	        if (!specificationList.isEmpty()) {
	        	spec = specificationList.get(0);
	        	for (int i=1; i < specificationList.size(); i++) {
	        		spec = Specification.where(spec).and(specificationList.get(i)); 
	        	}
	        	
	        	spec = Specification.where(spec).and(CollectionSpecification.hasUserWithId(user.getUserId()));
	        	spec = Specification.where(spec).and(CollectionSpecification.hasChildren());
	        	
	        	if (globalSpec != null) {
	        		spec = Specification.where(spec).and(globalSpec);
	        	}
	        }
	        if (sortByChildren) {
	        	if (specificationList.isEmpty()) {
	        		spec = Specification.where(spec).and(CollectionSpecification.hasUserWithId(user.getUserId()));
	        		spec = Specification.where(spec).and(CollectionSpecification.hasChildren());
	        	}
	        	spec = Specification.where (spec).and(
	        			CollectionSpecification.orderBySize(asc, "collections")); 
	        				
	        }
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
        	CollectionView cv = createCollectionView (c, imageLocation);
        	collections.add(cv);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("objects", collections);
        response.put("currentPage", collectionsInPage.getNumber());
        response.put("totalItems", collectionsInPage.getTotalElements());
        response.put("totalPages", collectionsInPage.getTotalPages());
        
        return response;
    }
    
    @Operation(summary = "Get collection by the given id", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/getcollection/{collectionId}")
    public ResponseEntity<SuccessResponse<CollectionView>> getCollectionById(
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
        
        CollectionView cv = createCollectionView (existing, imageLocation);
        
        return new ResponseEntity<>(new SuccessResponse<CollectionView>(cv, "collection retrieved"), HttpStatus.OK);
    }
    
    static CollectionView createCollectionView (Collection collection, String imageLocation) {
    	CollectionView cv = new CollectionView();
        cv.setCollectionId(collection.getCollectionId());
    	cv.setName(collection.getName());
    	if (collection.getType() == null) 
    		collection.setType(CollectionType.GLYCAN);
    	cv.setType(collection.getType());
    	cv.setDescription(collection.getDescription());
    	if (collection.getMetadata() != null) cv.setMetadata(new ArrayList<>(collection.getMetadata()));
    	if (collection.getTags() != null) cv.setTags(new ArrayList<>(collection.getTags()));
    	if (collection.getType() == CollectionType.GLYCAN) {
	    	if (collection.getGlycans() != null && !collection.getGlycans().isEmpty()) {
	    		cv.setGlycans(new ArrayList<Glycan>());
		    	for (GlycanInCollection gic: collection.getGlycans()) {
		    		Glycan g = gic.getGlycan();
		    		g.setGlycanCollections(null);
		    		g.setSites(null);
		    		SequenceUtils.addCompositionInformation(g);
		    		//g.setByonicString(SequenceUtils.generateByonicString(g));
		        	//g.setCondensedString(SequenceUtils.generateCondensedString(g));
		    		try {
		                g.setCartoon(getImageForGlycan(imageLocation, g.getGlycanId()));
		            } catch (DataNotFoundException e) {
		                // ignore
		                logger.warn ("no image found for glycan " + g.getGlycanId());
		            }
		    		cv.getGlycans().add(g);
		    	}
	    	}
    	} else {   // Glycoproteins
    		if (collection.getGlycoproteins() != null && !collection.getGlycoproteins().isEmpty()) {
    			cv.setGlycoproteins(new ArrayList<>());
	    		for (GlycoproteinInCollection gp: collection.getGlycoproteins()) {
	    			Glycoprotein p = gp.getGlycoprotein();
	    			for (Site s: p.getSites()) {
	    				for (GlycanInSite gic: s.getGlycans()) {
	    	        		Glycan g = gic.getGlycan();
	    	        		if (g != null) {
		    	        		g.setGlycanCollections(null);
		    	        		g.setSites(null);
		    	        		SequenceUtils.addCompositionInformation(g);
		    	        		//g.setByonicString(SequenceUtils.generateByonicString(g));
		    	            	//g.setCondensedString(SequenceUtils.generateCondensedString(g));
		    	        		try {
		    	                    g.setCartoon(getImageForGlycan(imageLocation, g.getGlycanId()));
		    	                } catch (DataNotFoundException e) {
		    	                    // ignore
		    	                    logger.warn ("no image found for glycan " + g.getGlycanId());
		    	                }
	    	        		}
	    	        	}
	    			}
	    			cv.getGlycoproteins().add(p);
	    		}
    		}
    	}
    	if (collection.getCollections() != null && !collection.getCollections().isEmpty()) {
    		cv.setChildren(new ArrayList<>());
	    	for (Collection c: collection.getCollections()) {
	    		CollectionView child = new CollectionView();
	    		child.setCollectionId(c.getCollectionId());
	    		child.setName(c.getName());
	    		if (c.getType() == null)
	    			c.setType(CollectionType.GLYCAN);
	    		child.setType(c.getType());
	    		child.setDescription(c.getDescription());
	    		if (c.getMetadata() != null) child.setMetadata(new ArrayList<>(c.getMetadata()));
	    		if (c.getTags() != null) child.setTags(new ArrayList<>(c.getTags()));
	    		if (c.getType() == CollectionType.GLYCAN) {
	    	    	if (c.getGlycans() != null && !c.getGlycans().isEmpty()) {
			        	child.setGlycans(new ArrayList<Glycan>());
			        	for (GlycanInCollection gic: c.getGlycans()) {
			        		Glycan g = gic.getGlycan();
			        		g.setGlycanCollections(null);
			        		g.setSites(null);
			        		SequenceUtils.addCompositionInformation(g);
			        		try {
			                    g.setCartoon(getImageForGlycan(imageLocation, g.getGlycanId()));
			                } catch (DataNotFoundException e) {
			                    // ignore
			                    logger.warn ("no image found for glycan " + g.getGlycanId());
			                }
			        		child.getGlycans().add(g);
			        	}
	    	    	}
	    		} else {
	    			if (c.getGlycoproteins() != null && !c.getGlycoproteins().isEmpty()) {
		    			child.setGlycoproteins(new ArrayList<>());
		        		for (GlycoproteinInCollection gp: c.getGlycoproteins()) {
		        			Glycoprotein p = gp.getGlycoprotein();
		        			for (Site s: p.getSites()) {
		        				for (GlycanInSite gic: s.getGlycans()) {
		        	        		Glycan g = gic.getGlycan();
		        	        		g.setGlycanCollections(null);
		        	        		g.setSites(null);
		        	        		SequenceUtils.addCompositionInformation(g);
		        	        		try {
		        	                    g.setCartoon(getImageForGlycan(imageLocation, g.getGlycanId()));
		        	                } catch (DataNotFoundException e) {
		        	                    // ignore
		        	                    logger.warn ("no image found for glycan " + g.getGlycanId());
		        	                }
		        	        	}
		        			}
		        			child.getGlycoproteins().add(p);
		        		}
	    			}
	    		}
	        	cv.getChildren().add(child);
	    	}
    	}
    	
    	return cv;
    }
    
    @Operation(summary = "Get collection of collections by the given id", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/getcoc/{collectionId}")
    public ResponseEntity<SuccessResponse<CollectionView>> getCoCById(
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
        
        CollectionView cv = createCollectionView(existing, imageLocation);
        
        return new ResponseEntity<>(new SuccessResponse<CollectionView>(cv, "collection retrieved"), HttpStatus.OK);
    }
    
   
    @Operation(summary = "Get batch uploads", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/checkbatchupload")
    @ApiResponses(value = { @ApiResponse(responseCode="200", description= "Check performed successfully", content = {
            @Content( schema = @Schema(implementation = SuccessResponse.class))}),
            @ApiResponse(responseCode="415", description= "Media type is not supported"),
            @ApiResponse(responseCode="500", description= "Internal Server Error") })
    public ResponseEntity<SuccessResponse<List<BatchUploadEntity>>> getActiveBatchUpload (
    		@Parameter(required=true, description="type of the batch upload", schema = @Schema(type = "string",
    	    		allowableValues={"GLYCOPROTEIN", "GLYCAN"}))
            @RequestParam("type")
    		String type) {
    	// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        CollectionType colType = CollectionType.valueOf(type);
        if (user != null) {
        	List<BatchUploadEntity> uploads = uploadRepository.findByUserOrderByStartDateDesc(user);	
        	List<BatchUploadEntity> filtered = new ArrayList<>();
        	if (uploads.isEmpty()) {
        		throw new DataNotFoundException("No active batch upload");
        	} else {
        		for (BatchUploadEntity upload: uploads) {
        			// check the type
        			if (upload.getType() == null || upload.getType() == colType) {
        				filtered.add(upload);
        			}
        		}
        		
        	}
        	if (filtered.isEmpty()) {
    			throw new DataNotFoundException("No active batch upload");
    		}
        	return new ResponseEntity<>(new SuccessResponse<List<BatchUploadEntity>>(filtered, "Batch uploads retrieved"), HttpStatus.OK);
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
    public ResponseEntity<SuccessResponse<BatchUploadEntity>> updateActiveBatchUpload(
    		@Parameter(required=true, description="internal id of the file upload to mark as read") 
            @PathVariable("uploadId")
    		Long batchUploadId){

    	Optional<BatchUploadEntity> upload = uploadRepository.findById(batchUploadId);
    	if (upload != null) {
            BatchUploadEntity entity = upload.get();
            entity.setAccessedDate(new Date());
            uploadRepository.save(entity);
            return new ResponseEntity<>(new SuccessResponse<BatchUploadEntity>(entity, "file upload is marked as read"), HttpStatus.OK);
        }
	
        throw new BadRequestException("file upload cannot be found");
    }
    
    @Operation(summary = "Delete the given file upload", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/deletefileupload/{uploadId}", method = RequestMethod.DELETE)
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="file upload deleted successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to delete file uploads"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<SuccessResponse<Long>> deleteBatchUpload (
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
        
        BatchUploadEntity toDelete = found.get(0);
        glycanManager.deleteBatchUpload(toDelete);
        return new ResponseEntity<>(new SuccessResponse<Long>(uploadId, "File upload deleted successfully"), HttpStatus.OK);
    }
    
    @Operation(summary = "Send error report email", 
            security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value = "/senderrorreport/{errorId}", method = RequestMethod.POST,
            produces={"application/json", "application/xml"})
    @ApiResponses(value = { @ApiResponse(responseCode="200", description= "Email sent successfully", content = {
            @Content( schema = @Schema(implementation = SuccessResponse.class))}),
            @ApiResponse(responseCode="415", description= "Media type is not supported"),
            @ApiResponse(responseCode="500", description= "Internal Server Error") })
    public ResponseEntity<SuccessResponse<UserError>> sendErrorReport(
    		@Parameter(required=true, description="internal id of the file upload") 
            @PathVariable("errorId")
    		Long errorId,
    		@Parameter(required=false, description="is report related to the upload")
    		@RequestParam (defaultValue="false")
    		Boolean isUpload) {
    	
        if (isUpload) {
        	Optional<BatchUploadEntity> upload = uploadRepository.findById(errorId);
        	if (upload != null) {
        		errorReportingService.reportUserError(upload.get());
        		return new ResponseEntity<>(new SuccessResponse<UserError>(upload.get(), "file upload report is sent"), HttpStatus.OK);
        	}
        	
        } else {
	    	Optional<UploadErrorEntity> upload = uploadErrorRepository.findById(errorId);
	    	if (upload != null) {
	    		errorReportingService.reportUserError(upload.get());
	            return new ResponseEntity<>(new SuccessResponse<UserError>(upload.get(), "file upload report is sent"), HttpStatus.OK);
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
    public ResponseEntity<SuccessResponse<BatchUploadEntity>> addTagForFileUpload(
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
            return new ResponseEntity<>(new SuccessResponse<BatchUploadEntity>(entity, "the tag is added to all glycans of this file upload"), HttpStatus.OK);
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
    public ResponseEntity<SuccessResponse<Glycan>> updateGlycanTags(
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
            return new ResponseEntity<>(new SuccessResponse<Glycan>(g, "the given tags are added to the given glycan"), HttpStatus.OK);
        }
	
        throw new BadRequestException("glycan cannot be found");
    }
    
    @Operation(summary = "Add tag for the given glycoprotein", 
            security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value = "/addglycoproteintag/{proteinId}", method = RequestMethod.POST,
            produces={"application/json", "application/xml"})
    @ApiResponses(value = { @ApiResponse(responseCode="200", description= "Update performed successfully", content = {
            @Content( schema = @Schema(implementation = SuccessResponse.class))}),
            @ApiResponse(responseCode="415", description= "Media type is not supported"),
            @ApiResponse(responseCode="500", description= "Internal Server Error") })
    public ResponseEntity<SuccessResponse<Glycoprotein>> updateGlycoproteinTags(
    		@Parameter(required=true, description="internal id of the glycoprotein") 
            @PathVariable("proteinId")
    		Long glycoproteinId,
    		@RequestBody List<String> tags){
    	
    	if (tags == null) {
    		throw new IllegalArgumentException("Tags cannnot be emnpty");
    	}
    	
    	// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }

    	Optional<Glycoprotein> prot = glycoproteinRepository.findById(glycoproteinId);
    	if (prot != null) {
            Glycoprotein g = prot.get();
            glycanManager.setGlycoproteinTags(g, tags, user);
            return new ResponseEntity<>(new SuccessResponse<Glycoprotein>(g, "the given tags are added to the given glycoprotein"), HttpStatus.OK);
        }
	
        throw new BadRequestException("glycoprotein cannot be found");
    }
    

    @Operation(summary = "Get current glycan tags for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/getglycantags")
    public ResponseEntity<SuccessResponse<List<GlycanTag>>> getGlycanTags() {
    	// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }

    	List<GlycanTag> tags = glycanManager.getTags(user);
    	return new ResponseEntity<>(new SuccessResponse<List<GlycanTag>>(tags, "user's glycan tags retrieved successfully"), HttpStatus.OK);
    }
    
    @Operation(summary = "Delete given glycan from the user's list", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/deleteglycan/{glycanId}", method = RequestMethod.DELETE)
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Glycan deleted successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to delete glycans"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<SuccessResponse<Long>> deleteGlycan (
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
        //need to check if the glycan appears in any glycoprotein collection and give an error message
        if (existing.getSites() != null && !existing.getSites().isEmpty()) {
        	String collectionString = "";
        	for (GlycanInSite site: existing.getSites()) {
        		for (GlycoproteinInCollection col: site.getSite().getGlycoprotein().getGlycoproteinCollections()) {
        			collectionString += col.getCollection().getName() + ", ";
        		}
        	}
        	if (collectionString.endsWith(", ")) {
	        	collectionString = collectionString.substring(0, collectionString.lastIndexOf(","));
	        	throw new BadRequestException ("Cannot delete this glycan. It is used in the following collections: " + collectionString);
        	}
        }
        glycanRepository.deleteById(glycanId);
        return new ResponseEntity<>(new SuccessResponse<Long>(glycanId, "Glycan deleted successfully"), HttpStatus.OK);
    }
    
    @Operation(summary = "Delete given glycans from the user's list", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/deletemultipleglycans", method = RequestMethod.POST)
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Glycan deleted successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to delete glycans"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<SuccessResponse<Integer[]>> deleteMultipleGlycans (
            @Parameter(required=true, description="list of internal ids of the glycans to delete") 
            @RequestBody Integer[] glycanIds) {
        
        // get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        
        StringBuffer errorMessage = new StringBuffer();
        for (Integer glycanId: glycanIds) {
	        Glycan existing = glycanRepository.findByGlycanIdAndUser(glycanId.longValue(), user);
	        if (existing == null) {
	            errorMessage.append("Could not find the given glycan " + glycanId + " for the user\n");
	            continue;
	        }
	        //need to check if the glycan appears in any collection and give an error message
	        if (existing.getGlycanCollections() != null && !existing.getGlycanCollections().isEmpty()) {
	        	String collectionString = "";
	        	for (GlycanInCollection col: existing.getGlycanCollections()) {
	        		collectionString += col.getCollection().getName() + ", ";
	        	}
	        	collectionString = collectionString.substring(0, collectionString.lastIndexOf(","));
	        	errorMessage.append ("Cannot delete " + (existing.getGlytoucanID() != null ?  existing.getGlytoucanID() : existing.getGlycanId()) 
	        			+ ". It is used in the following collections: " + collectionString + "\n");
	        	continue;
	        }
	        //need to check if the glycan appears in any glycoprotein collection and give an error message
	        if (existing.getSites() != null && !existing.getSites().isEmpty()) {
	        	String collectionString = "";
	        	String glycoproteinString = "";
	        	for (GlycanInSite site: existing.getSites()) {
	        		for (GlycoproteinInCollection col: site.getSite().getGlycoprotein().getGlycoproteinCollections()) {
	        			collectionString += col.getCollection().getName() + ", ";
	        		}
	        		glycoproteinString += (site.getSite().getGlycoprotein().getName() != null ?  site.getSite().getGlycoprotein().getName() + ", " 
	        				: site.getSite().getGlycoprotein().getUniprotId() + ", ");
	        		
	        	}
	        	if (collectionString.endsWith (", ")) {
	        		collectionString = collectionString.substring(0, collectionString.lastIndexOf(","));
	        		errorMessage.append("Cannot delete " + (existing.getGlytoucanID() != null ?  existing.getGlytoucanID() : existing.getGlycanId())
	        			+ ". It is used in the following collections: " + collectionString + "\n");
	        		continue;
	        	}
	        	if (glycoproteinString.endsWith (", ")) {
	        		glycoproteinString = glycoproteinString.substring(0, glycoproteinString.lastIndexOf(","));
	        		errorMessage.append("Cannot delete " + (existing.getGlytoucanID() != null ?  existing.getGlytoucanID() : existing.getGlycanId())
	        			+ ". It is referenced in the following glycoproteins: " + glycoproteinString + "\n");
	        		continue;
	        	}
	        }
	        glycanRepository.deleteById(glycanId.longValue());
        }
        if (!errorMessage.isEmpty()) {
        	throw new BadRequestException(errorMessage.toString());
        }
        return new ResponseEntity<>(new SuccessResponse<Integer[]>(glycanIds, "Glycans deleted successfully"), HttpStatus.OK);
    }
    
    @Operation(summary = "Delete given glycoprotein from the user's list", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/deleteglycoprotein/{proteinId}", method = RequestMethod.DELETE)
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Glycan deleted successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to delete glycoproteins"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<SuccessResponse<Long>> deleteGlycoprotein (
            @Parameter(required=true, description="internal id of the glycoprotein to delete") 
            @PathVariable("proteinId") Long glycoproteinId) {
        
        // get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        Glycoprotein existing = glycoproteinRepository.findByIdAndUser(glycoproteinId, user);
        if (existing == null) {
            throw new IllegalArgumentException ("Could not find the given glycoprotein " + glycoproteinId + " for the user");
        }
        //need to check if the glycan appears in any collection and give an error message
        if (existing.getGlycoproteinCollections() != null && !existing.getGlycoproteinCollections().isEmpty()) {
        	String collectionString = "";
        	for (GlycoproteinInCollection col: existing.getGlycoproteinCollections()) {
        		collectionString += col.getCollection().getName() + ", ";
        	}
        	collectionString = collectionString.substring(0, collectionString.lastIndexOf(","));
        	throw new BadRequestException ("Cannot delete this glycoprotein. It is used in the following collections: " + collectionString);
        }
        glycoproteinRepository.deleteById(glycoproteinId);
        return new ResponseEntity<>(new SuccessResponse<Long>(glycoproteinId, "Glycoprotein deleted successfully"), HttpStatus.OK);
    }
    
    @Operation(summary = "Delete the given collection from the user's list", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/deletecollection/{collectionId}", method = RequestMethod.DELETE)
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Collection deleted successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to delete collections"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<SuccessResponse<Long>> deleteCollection (
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
        return new ResponseEntity<>(new SuccessResponse<Long>(collectionId, "Collection deleted successfully"), HttpStatus.OK);
    }
    
    @Operation(summary = "Delete the given collection of collections from the user's list", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/deletecoc/{collectionId}", method = RequestMethod.DELETE)
    @ApiResponses (value ={@ApiResponse(responseCode="200", description="Collection deleted successfully"), 
            @ApiResponse(responseCode="401", description="Unauthorized"),
            @ApiResponse(responseCode="403", description="Not enough privileges to delete collections"),
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
    public ResponseEntity<SuccessResponse<Long>> deleteCoC (
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
        
        for (Collection child: existing.getCollections()) {
        	collectionRepository.delete(child);
        }
        existing.getCollections().clear();
        collectionRepository.delete(existing);
        return new ResponseEntity<>(new SuccessResponse<Long>(collectionId, "Collection of collections deleted successfully"), HttpStatus.OK);
    }
    
    @Operation(summary = "Add given glycans", security = { @SecurityRequirement(name = "bearer-key") })
    @PostMapping("/addglycanfromlist")
    public ResponseEntity<SuccessResponse<List<Glycan>>> addGlycansFromList (@RequestBody List<GlycanView> gList,
    		@Parameter(required=false, description="composition type")
			@RequestParam (required=false, defaultValue="BASE")
			CompositionType compositionType,
			@Parameter(required=false, description="tag for the glycans")
			@RequestParam (required=false)
			String tag) {
    	
    	// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        
        StringBuffer errors = new StringBuffer();
        
    	List<Glycan> allAdded = new ArrayList<>();
    	int i=0;
    	for (GlycanView g: gList) {
    		try {
	    		ResponseEntity<SuccessResponse<Glycan>> response = addGlycan (g, g.getType() != null ? g.getType() : compositionType);
	    		Glycan added = response.getBody().getData();
	    		allAdded.add(added);
    		} catch (DuplicateException e) {
    			allAdded.add ((Glycan)e.getDuplicate());
    		} catch (Exception e) {
    			// add them to error list and return errors
    			errors.append ("Row " + i + ": " + e.getMessage()+ ";");
    		}
    		i++;
    	}
    	
    	glycanManager.addTagToGlycans(allAdded, tag, user);
    	
    	if (errors.isEmpty()) {
    		return new ResponseEntity<>(new SuccessResponse<List<Glycan>>(allAdded, "glycans added"), HttpStatus.OK);
    	} else {
    		throw new IllegalArgumentException ("Errors: " + errors.toString());
    	}
    }

    
    @Operation(summary = "Add glycan", security = { @SecurityRequirement(name = "bearer-key") })
    @PostMapping("/addglycan")
    public ResponseEntity<SuccessResponse<Glycan>> addGlycan(@Valid @RequestBody GlycanView g,
    		@Parameter(required=false, description="composition type")
			@RequestParam (required=false, defaultValue="BASE")
			CompositionType compositionType) {
        if ((g.getSequence() == null || g.getSequence().isEmpty()) 
                && (g.getGlytoucanID() == null || g.getGlytoucanID().isEmpty())
                && (g.getComposition() == null || g.getComposition().isEmpty())) {
            throw new IllegalArgumentException("Either the sequence or GlyToucan id or composition string must be provided to add glycans");
        }
        if (compositionType == null) {
        	compositionType = CompositionType.BASE;
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
            List<Glycan> existing = glycanRepository.findByGlytoucanIDIgnoreCaseAndUser(g.getGlytoucanID().trim(), user);
            if (existing != null && existing.size() > 0) {
                throw new DuplicateException ("There is already a glycan with GlyTouCan ID " + g.getGlytoucanID(), null, existing.get(0));
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
            parseAndRegisterGlycan(glycan, g, glycanRepository, errorReportingService, user);
        } else { // composition
            try {
                
                String strWURCS = null;
                switch (compositionType) {
                case BASE:
                	Composition compo = CompositionUtils.parse(g.getComposition().trim());
                	strWURCS = CompositionConverter.toWURCS(compo);
                	strWURCS = toUnknownForm(strWURCS);
                	break;
                case GLYGEN:
                	compo = CompositionUtils.parse(g.getComposition().trim());
                	SugarToWURCSGraph t_s2w = new SugarToWURCSGraph();
        			try {
        				Sugar sugar = CompositionConverter.toSugar(compo);
        				for (GlycoNode node: sugar.getNodes()) {
        					if (node instanceof Monosaccharide) {
        						Monosaccharide m = ((Monosaccharide)node);
        						m.setRing(-1, -1);
        						m.setAnomer(Anomer.Unknown);
        					}
        				}
        				t_s2w.start(sugar);
        			} catch (WURCSExchangeException | GlycoconjugateException e) {
        				throw new ConversionException("Error in converting composition to Sugar object.", e);
        			}
        			try {
        				WURCSFactory factory = new WURCSFactory(t_s2w.getGraph());
        				strWURCS = factory.getWURCS();
        			} catch (WURCSException e) {
        				throw new ConversionException("Error in encoding the composition in WURCS.", e);
        			}
                	break;
                case DEFINED:
                	compo = CompositionUtils.parse(g.getComposition().trim());
                	strWURCS = CompositionConverter.toWURCS(compo);
                	break;
				case BYONIC:
					compo = SequenceUtils.getWurcsCompositionFromByonic(g.getComposition().trim());
					strWURCS = CompositionConverter.toWURCS(compo);
					break;
				case COMPACT:
					compo = SequenceUtils.getWurcsCompositionFromCondensed(g.getComposition().trim());
					strWURCS = CompositionConverter.toWURCS(compo);
					break;
				default:
					break;
                }
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
                List<Glycan> existing = glycanRepository.findByWurcsIgnoreCaseAndUser(glycan.getWurcs(), user);
                if (existing != null && existing.size() > 0) {
                    throw new DuplicateException ("There is already a glycan with WURCS " + glycan.getWurcs(), null, existing.get(0));
                }
                try {
                	SequenceUtils.getWurcsAndGlytoucanID(glycan, null);
                	if (glycan.getGlytoucanID() == null || glycan.getGlytoucanID().isEmpty()) {
                    	SequenceUtils.registerGlycan(glycan);
                    }
                } catch (GlytoucanAPIFailedException e) {
                	glycan.setStatus(RegistrationStatus.NOT_SUBMITTED_YET);
                	//glycan.setError("Cannot retrieve glytoucan id. Reason: " + e.getMessage());
                	// report the error through email
                	ErrorReportEntity error = new ErrorReportEntity();
    				error.setMessage(e.getMessage());
    				error.setDetails("Error occurred in AddGlycan");
    				error.setDateReported(new Date());
    				error.setTicketLabel("GlytoucanAPI");
    				errorReportingService.reportError(error);
                }
                
            } catch (DictionaryException | CompositionParseException | ConversionException | WURCSException e1) {
                throw new IllegalArgumentException (e1.getMessage());
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
            GlycanImageEntity imageEntity = new GlycanImageEntity();
            imageEntity.setGlycanId(added.getGlycanId());
            imageEntity.setGlytoucanId(added.getGlytoucanID());
            imageEntity.setWurcs(added.getWurcs());
            glycanImageRepository.save(imageEntity);
        } 
        return new ResponseEntity<>(new SuccessResponse<Glycan>(glycan, "glycan added"), HttpStatus.OK);
    }
    
    private static String toUnknownForm(String strWURCS) throws WURCSException {

        WURCSFactory factory = new WURCSFactory(strWURCS);
        WURCSArray array = factory.getArray();

        // copy array without UniqueRES
        WURCSArray arrayNew = new WURCSArray(
            array.getVersion(),
            array.getUniqueRESCount(),
            array.getRESCount(),
            array.getLINCount(),
            array.isComposition()
        );

        for (RES res : array.getRESs())
          arrayNew.addRES(res);

        for ( LIN lin : array.getLINs() )
          arrayNew.addLIN(lin);

        // add UniqueRES converted to unknown form
        WURCSSubsumptionConverter converter = new WURCSSubsumptionConverter();
        for (UniqueRES ures : array.getUniqueRESs()) {
          MS ms = converter.convertAnomericCarbonToUncertain(ures);
          UniqueRES uresNew = new UniqueRES(ures.getUniqueRESID(), ms);
          arrayNew.addUniqueRES(uresNew);
        }

        // back to WURCS
        WURCSFactory factoryNew = new WURCSFactory(arrayNew);
        return factoryNew.getWURCS();
    }

    
    @Operation(summary = "Add collection", security = { @SecurityRequirement(name = "bearer-key") })
    @PostMapping("/addcollection")
    public ResponseEntity<SuccessResponse<CollectionView>> addCollection(@Valid @RequestBody CollectionView c) {
    	// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        
        // check if duplicate
    	List<Collection> existing = collectionRepository.findAllByNameAndUser(c.getName(), user);
    	if (existing.size() > 0) {
    		throw new DuplicateException("A collection with this name already exists! ");
    	}
    	
    	Collection collection = new Collection();
    	collection.setName(c.getName());
    	collection.setDescription(c.getDescription());
    	collection.setType(c.getType());
    	
    	if (c.getGlycans() != null && !c.getGlycans().isEmpty()) {
    		collection.setGlycans(new ArrayList<>());
    		for (Glycan g: c.getGlycans()) {
    			if (g.getGlycanId() != null) {
    				// retrieve the glycan
    				Optional<Glycan> ge = glycanRepository.findById(g.getGlycanId());
    				if (ge.isPresent()) {
    					GlycanInCollection gic = new GlycanInCollection();
    	    			gic.setCollection(collection);
    	    			gic.setGlycan(ge.get());
    	    			gic.setDateAdded(new Date());
    	    			collection.getGlycans().add(gic);
    	    		} else {
    	    			throw new IllegalArgumentException ("Could not find the given glycan " + g.getGlycanId());
    	    		}
    			} else {
	    			GlycanInCollection gic = new GlycanInCollection();
	    			gic.setCollection(collection);
	    			gic.setGlycan(g);
	    			gic.setDateAdded(new Date());
	    			collection.getGlycans().add(gic);
    			}
    		}
    	}
    	
    	if (c.getGlycoproteins() != null && !c.getGlycoproteins().isEmpty()) {
    		collection.setGlycoproteins(new ArrayList<>());
    		for (Glycoprotein g: c.getGlycoproteins()) {
    			GlycoproteinInCollection gic = new GlycoproteinInCollection();
    			gic.setCollection(collection);
    			gic.setGlycoprotein(g);
    			gic.setDateAdded(new Date());
    			collection.getGlycoproteins().add(gic);
    		}
    	}
    	
        collection.setUser(user);
    	Collection saved = collectionRepository.save(collection);
    	
    	if (c.getMetadata() != null) {
    		List<Metadata> metadataList = new ArrayList<>();
    		for (Metadata m: c.getMetadata()) {
    			Metadata newMetadata = null;
    			if (m.getMetadataId() != null) { // need to copy
    				newMetadata = new Metadata();
    				newMetadata.setType(m.getType());
    				newMetadata.setValue(m.getValue());
    				newMetadata.setValueId(m.getValueId());
    				newMetadata.setValueUri(m.getValueUri());
    			} else {
    				newMetadata = m;
    			}
    			newMetadata.setCollection(saved);
    			if (newMetadata.getValueUri() != null) {
					// last part of the uri is the id, either ../../<id> or ../../id=<id>
					if (newMetadata.getValueUri().contains("id=")) {
						newMetadata.setValueId (newMetadata.getValueUri().substring(newMetadata.getValueUri().indexOf("id=")+3));
					} else {
						newMetadata.setValueId (newMetadata.getValueUri().substring(newMetadata.getValueUri().lastIndexOf("/")+1));
					}
				}
    			metadataList.add(newMetadata);
    			
    		}
    		saved.setMetadata(metadataList);
    		saved = collectionManager.saveCollectionWithMetadata(saved);
    	}
    	CollectionView sv = createCollectionView(saved, imageLocation);
    	return new ResponseEntity<>(new SuccessResponse<CollectionView>(sv, "collection added"), HttpStatus.OK);
    }
    
    @Operation(summary = "Add collection of collections", security = { @SecurityRequirement(name = "bearer-key") })
    @PostMapping("/addcoc")
    public ResponseEntity<SuccessResponse<CollectionView>> addCoC(@Valid @RequestBody CollectionView c) {
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
    	List<Collection> existing = collectionRepository.findAllByNameAndUser(c.getName(), user);
    	if (existing.size() > 0) {
    		throw new DuplicateException("A collection of collections with this name already exists!");
    	}
    	
    	// check if all selected collections are of the same type
    	CollectionType type = c.getType();
    	for (CollectionView child: c.getChildren()) {
    		if (child.getType() != type) {
    			// error
    			throw new IllegalArgumentException("All selected collections should be of the same type, either all Glycan or all Glyccoprotein!");
    		}
    	}
        
    	Collection collection = new Collection();
    	collection.setType(c.getType());
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
    	return new ResponseEntity<>(new SuccessResponse<CollectionView>(c, "collection added"), HttpStatus.OK);
    }
    
    @Operation(summary = "Add glycoprotein", security = { @SecurityRequirement(name = "bearer-key") })
    @PostMapping("/addglycoprotein")
    public ResponseEntity<SuccessResponse<Glycoprotein>> addGlycoprotein(@RequestBody GlycoproteinView gp) {
    	// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        
        Glycoprotein saved = addGlycoprotein(gp, user, glycoproteinRepository);
    	return new ResponseEntity<>(new SuccessResponse<Glycoprotein>(saved, "glycoprotein added"), HttpStatus.OK);
    }
    
    public static Glycoprotein addGlycoprotein (GlycoproteinView gp, UserEntity user, GlycoproteinRepository glycoproteinRepository) {
    	if (gp.getName() != null && !gp.getName().isEmpty()) {
	       	 // check if duplicate
	       	List<Glycoprotein> existing = glycoproteinRepository.findAllByNameAndUser(gp.getName(), user);
	       	if (existing.size() > 0) {
	       		throw new DuplicateException("A glycoprotein with this name already exists! ");
	       	}
    	}
   	
	   	Glycoprotein glycoprotein = new Glycoprotein();
	   	glycoprotein.setName(gp.getName());
	   	glycoprotein.setProteinName(gp.getProteinName());
	   	glycoprotein.setGeneSymbol(gp.getGeneSymbol());
	   	glycoprotein.setUniprotId(gp.getUniprotId());
	   	glycoprotein.setSequence(gp.getSequence());
	   	glycoprotein.setSequenceVersion(gp.getSequenceVersion());
	   	glycoprotein.setTags(gp.getTags());
	   	glycoprotein.setUser(user);
	   	glycoprotein.setDateCreated(new Date());
	   	glycoprotein.setSites(new ArrayList<>());
	   	if (gp.getSites() != null) {
	   		for (SiteView sv: gp.getSites()) {
	   			Site s = new Site();
	   			s.setType(sv.getType());
	   			s.setGlycoprotein(glycoprotein);
	   			if (sv.getPosition() != null && sv.getType() != GlycoproteinSiteType.UNKNOWN) {
	   				s.setPositionString(sv.getPosition().toString()); // convert the position to JSON string
	   			}
	   			else if (sv.getPosition() == null && sv.getType() != GlycoproteinSiteType.UNKNOWN) {
	   				// error
	   				throw new IllegalArgumentException("Position cannot be left empty if the site type is not unknown");
	   			}
	   			s.setGlycans(new ArrayList<>());
	   			if (sv.getGlycans() != null && !sv.getGlycans().isEmpty()) {
	   				for (GlycanInSiteView gv: sv.getGlycans()) {
	   					GlycanInSite g = new GlycanInSite();
	   					g.setGlycan(gv.getGlycan());
	   					g.setSite (s);
	   					g.setGlycosylationSubType(gv.getGlycosylationSubType());
	   					g.setGlycosylationType(gv.getGlycosylationType());
	   					g.setType(gv.getType());
	   					s.getGlycans().add(g);
	   				}
	   			} else if (sv.getGlycosylationType() != null && !sv.getGlycosylationType().isEmpty()) {
	   				GlycanInSite g = new GlycanInSite();
	   				g.setGlycosylationSubType(sv.getGlycosylationSubType());
	   				g.setGlycosylationType(sv.getGlycosylationType());
	   				g.setSite (s);
	   				s.getGlycans().add(g);
	   			}
	   			glycoprotein.getSites().add(s);
	   		}
	   	}
	   	Glycoprotein saved = glycoproteinRepository.save(glycoprotein);
	   	return saved;
    }
    
    @Operation(summary = "Get glycorotein by the given id", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/getglycoprotein/{glycoproteinId}")
    public ResponseEntity<SuccessResponse<GlycoproteinView>> getGlycoproteinById(
    		@Parameter(required=true, description="id of the collection to be retrieved") 
    		@PathVariable("glycoproteinId") Long glycoproteinId) {
    	// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        Glycoprotein existing = glycoproteinRepository.findByIdAndUser(glycoproteinId, user);
        if (existing == null) {
            throw new IllegalArgumentException ("Could not find the given glycoprotein " + glycoproteinId + " for the user");
        }
         
        if (existing.getSites() != null) {
    		for (Site s: existing.getSites()) {
    			if (s.getPositionString() != null) {
    				ObjectMapper om = new ObjectMapper();
    				try {
						s.setPosition(om.readValue(s.getPositionString(), SitePosition.class));
					} catch (JsonProcessingException e) {
						logger.warn ("Position string is invalid: " + s.getPositionString());
					}
    			}
    			
				for (GlycanInSite gic: s.getGlycans()) {
	        		Glycan g = gic.getGlycan();
	        		if (g != null) {
    	        		g.setGlycanCollections(null);
    	        		g.setSites(null);
    	        		SequenceUtils.addCompositionInformation(g);
    	        		//g.setByonicString(SequenceUtils.generateByonicString(g));
    	            	//g.setCondensedString(SequenceUtils.generateCondensedString(g));
    	        		try {
    	                    g.setCartoon(getImageForGlycan(imageLocation, g.getGlycanId()));
    	                } catch (DataNotFoundException e) {
    	                    // ignore
    	                    logger.warn ("no image found for glycan " + g.getGlycanId());
    	                }
	        		}
	        	}
    		}
    	}
        GlycoproteinView gv = new GlycoproteinView(existing);
        
        return new ResponseEntity<>(new SuccessResponse<GlycoproteinView>(gv, "glycoprotein retrieved"), HttpStatus.OK);
    }
    
    @Operation(summary = "Get glycan by sequence", security = { @SecurityRequirement(name = "bearer-key") })
    @PostMapping("/getglycan")
    public ResponseEntity<SuccessResponse<Glycan>> getGlycanBySequence(
    		@Valid @RequestBody GlycanView g) {
    	// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        
        List<Glycan> existing = new ArrayList<>();
        switch (g.getFormat()) {
        case GLYCOCT:
        	existing = glycanRepository.findByGlycoCTIgnoreCaseAndUser(g.getSequence(), user);
        	break;
        case WURCS:
        	existing = glycanRepository.findByWurcsIgnoreCaseAndUser(g.getSequence(), user);
        	break;
        case GWS:
        	existing = glycanRepository.findByGwsIgnoreCaseAndUser(g.getSequence(), user);
        	break;
		default:
			break;
        }
        
        if (existing.isEmpty()) {
        	throw new EntityNotFoundException("glycan with the given sequence does not exist!");
        }
        
        return new ResponseEntity<>(new SuccessResponse<Glycan>(existing.get(0), "glycan retrieved"), HttpStatus.OK);
    }
    
    @Operation(summary = "Get collection by name", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/getcollectionbyname")
    public ResponseEntity<SuccessResponse<CollectionView>> getCollectionByName(
    		@RequestParam(required=true, value="name")
    		String name) {
    	// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        
        List<Collection> existing = collectionRepository.findAllByNameAndUser (name, user);
        if (existing.isEmpty()) {
        	throw new EntityNotFoundException("collection with the given name does not exist!");
        }
        CollectionView result = createCollectionView(existing.get(0), imageLocation);
        return new ResponseEntity<>(new SuccessResponse<CollectionView>(result, "collection retrieved"), HttpStatus.OK);
    }

	@Operation(summary = "update collection", security = { @SecurityRequirement(name = "bearer-key") })
    @PostMapping("/updatecollection")
    public ResponseEntity<SuccessResponse<CollectionView>> updateCollection(@Valid @RequestBody CollectionView c) {
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
    	
    	if (existing.getType() == null)    // legacy data
    		existing.setType(CollectionType.GLYCAN);
    	existing.setName(c.getName());
    	existing.setDescription(c.getDescription());
    	
    	switch (existing.getType()) {
    	case GLYCAN:
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
        	break;
    		
		case GLYCOPROTEIN:
			if (existing.getGlycoproteins() == null) {
	    		existing.setGlycoproteins(new ArrayList<>());
	    	}
	    	
	    	if (c.getGlycoproteins() == null || c.getGlycoproteins().isEmpty()) {
	    		existing.getGlycoproteins().clear();
	    	} else {
		    	// remove glycans as necessary
	    		List<GlycoproteinInCollection> toBeRemoved = new ArrayList<>();
		    	for (GlycoproteinInCollection gic: existing.getGlycoproteins()) {
		    		boolean found = false;
		    		for (Glycoprotein g: c.getGlycoproteins()) {
		    			if (g.getId().equals(gic.getGlycoprotein().getId())) {
		    				// keep it
		    				found = true;
		    			}
		    		}
		    		if (!found) {
		    			toBeRemoved.add(gic);
		    		}
		    	}
		    	existing.getGlycoproteins().removeAll(toBeRemoved);
	    	}
	    	
	    	if (c.getGlycoproteins() != null && !c.getGlycoproteins().isEmpty()) {
	    		// check if this glycan already exists in the collection
	    		for (Glycoprotein g: c.getGlycoproteins()) {
	    			boolean exists = false;
	    			for (GlycoproteinInCollection gic: existing.getGlycoproteins()) {
	        			if (gic.getGlycoprotein().getId().equals(g.getId())) {
	        				exists = true;
	        				break;
	        			}
	        		}
	    			if (!exists) {
		    			GlycoproteinInCollection gic = new GlycoproteinInCollection();
		    			gic.setCollection(existing);
		    			gic.setGlycoprotein(g);
		    			gic.setDateAdded(new Date());
		    			existing.getGlycoproteins().add(gic);
	    			}
	    		}
	    	}
			break;
		default:
			break;
    	}
    	
    	if (existing.getMetadata() == null) {
    		existing.setMetadata(new ArrayList<>());
    	}
    	
    	if (c.getMetadata() == null || c.getMetadata().isEmpty()) {
    		existing.getMetadata().clear();
    	} else {
	    	// remove metadata as necessary
    		List<Metadata> toBeRemoved = new ArrayList<>();
	    	for (Metadata metadata: existing.getMetadata()) {
	    		boolean found = false;
	    		for (Metadata m: c.getMetadata()) {
	    			if (metadata.getMetadataId().equals(m.getMetadataId())) {
	    				// keep it
	    				found = true;
	    			}
	    		}
	    		if (!found) {
	    			toBeRemoved.add(metadata);
	    		}
	    	}
	    	existing.getMetadata().removeAll(toBeRemoved);
    	}
    	
    	if (c.getMetadata() != null && !c.getMetadata().isEmpty()) {
    		// check if this metadata already exists in the collection
    		for (Metadata m: c.getMetadata()) {
    			boolean exists = false;
    			for (Metadata metadata: existing.getMetadata()) {
        			if (metadata.getMetadataId() != null && metadata.getMetadataId().equals(m.getMetadataId())) {
        				exists = true;
        				// update the value
        				metadata.setValue(m.getValue());
        				metadata.setValueUri(m.getValueUri());
        				if (m.getValueUri() != null) {
        					// last part of the uri is the id, either ../../<id> or ../../id=<id>
        					if (m.getValueUri().contains("id=")) {
        						metadata.setValueId (m.getValueUri().substring(m.getValueUri().indexOf("id=")+3));
        					} else {
        						metadata.setValueId (m.getValueUri().substring(m.getValueUri().lastIndexOf("/")+1));
        					}
        				}
        				break;
        			}
        		}
    			if (!exists) {
    				Metadata newMetadata = new Metadata();
    				newMetadata.setCollection(existing);
    				newMetadata.setType(m.getType());
    				newMetadata.setValue(m.getValue());
    				newMetadata.setValueUri(m.getValueUri());
    				if (m.getValueUri() != null) {
    					// last part of the uri is the id, either ../../<id> or ../../id=<id>
    					if (m.getValueUri().contains("id=")) {
    						newMetadata.setValueId (m.getValueUri().substring(m.getValueUri().indexOf("id=")+3));
    					} else {
    						newMetadata.setValueId (m.getValueUri().substring(m.getValueUri().lastIndexOf("/")+1));
    					}
    				}
	    			existing.getMetadata().add(newMetadata);
    			}
    		}
    		UtilityController.getCanonicalForm (namespaceRepository, existing.getMetadata());
    	}
    	Collection saved = collectionManager.saveCollectionWithMetadata(existing);
    	CollectionView cv = createCollectionView(saved, imageLocation);
    	return new ResponseEntity<>(new SuccessResponse<CollectionView>(cv, "collection updated"), HttpStatus.OK);
    }
    
    @Operation(summary = "update collection of collections", security = { @SecurityRequirement(name = "bearer-key") })
    @PostMapping("/updatecoc")
    public ResponseEntity<SuccessResponse<CollectionView>> updateCoC(@Valid @RequestBody CollectionView c) {
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
    	
    	Collection saved = collectionRepository.save(existing);
    	CollectionView cv = createCollectionView(saved, imageLocation);
    	return new ResponseEntity<>(new SuccessResponse<CollectionView>(cv, "collection of collections updated"), HttpStatus.OK);
    }
    
    @Operation(summary = "update glycoprotein", security = { @SecurityRequirement(name = "bearer-key") })
    @PostMapping("/updateglycoprotein")
    public ResponseEntity<SuccessResponse<GlycoproteinView>> updateGlycoprotein(@Valid @RequestBody GlycoproteinView gv) {
    	if (gv.getId() == null) {
    		throw new IllegalArgumentException("collection id should be provided for update");
    	}
    	// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
    	Glycoprotein existing = glycoproteinRepository.findByIdAndUser(gv.getId(), user);
    	if (existing == null) {
    		throw new IllegalArgumentException("Glycoprotein (" + gv.getId() + ") to be updated cannot be found");
    	}
    	// check if name is a duplicate
    	if (existing.getName() != null && gv.getName()!= null && !existing.getName().equalsIgnoreCase(gv.getName())) {   // changing the name
	    	List<Glycoprotein> duplicate = glycoproteinRepository.findAllByNameAndUser (gv.getName(), user);
	    	if (!duplicate.isEmpty()) {
	    		throw new DuplicateException("Glycoprotein with name: " + gv.getName() + " already exists! Pick a different name");
	    	}
    	}
    	existing.setName(gv.getName());
    	
    	if (existing.getSites() == null) {
    		existing.setSites(new ArrayList<>());
    	}
    	
    	if (gv.getSites() == null || gv.getSites().isEmpty()) {
    		existing.getSites().clear();
    	} else {
	    	// remove sites as necessary
    		List<Site> toBeRemoved = new ArrayList<>();
	    	for (Site site: existing.getSites()) {
	    		boolean found = false;
	    		for (SiteView col: gv.getSites()) {
	    			if (site.getSiteId().equals(col.getSiteId())) {
	    				// keep it
	    				found = true;
	    			}
	    		}
	    		if (!found) {
	    			toBeRemoved.add(site);
	    		}
	    	}
	    	existing.getSites().removeAll(toBeRemoved);
    	}
    	
    	if (gv.getSites() != null && !gv.getSites().isEmpty()) {
    		List<Site> toBeAdded = new ArrayList<>();
    		// check if this site already exists in the glycoprotein
    		for (SiteView sv: gv.getSites()) {
    			boolean exists = false;
    			for (Site col: existing.getSites()) {
        			if (col.getSiteId().equals(sv.getSiteId())) {
        				exists = true;
        				break;
        			}
        		}
    			if (!exists) {
    				Site s = new Site();
        			s.setType(sv.getType());
        			s.setGlycoprotein(existing);
        			if (s.getPosition() != null && sv.getType() != GlycoproteinSiteType.UNKNOWN) {
        				s.setPositionString(sv.getPosition().toString()); // convert the position to JSON string
        			}
        			else if (sv.getPosition() == null && sv.getType() != GlycoproteinSiteType.UNKNOWN) {
    	   				// error
    	   				throw new IllegalArgumentException("Position cannot be left empty if the site type is not unknown");
    	   			}
        			s.setGlycans(new ArrayList<>());
        			if (sv.getGlycans() != null && !sv.getGlycans().isEmpty()) {
        				for (GlycanInSiteView giv: sv.getGlycans()) {
        					GlycanInSite g = new GlycanInSite();
        					g.setGlycan(giv.getGlycan());
        					g.setSite (s);
        					g.setGlycosylationSubType(giv.getGlycosylationSubType());
        					g.setGlycosylationType(giv.getGlycosylationType());
        					g.setType(giv.getType());
        					s.getGlycans().add(g);
        				}
        			} else if (sv.getGlycosylationType() != null && !sv.getGlycosylationType().isEmpty()) {
        				GlycanInSite g = new GlycanInSite();
        				g.setGlycosylationSubType(sv.getGlycosylationSubType());
        				g.setGlycosylationType(sv.getGlycosylationType());
        				g.setSite (s);
        				s.getGlycans().add(g);
        			}
    				toBeAdded.add(s);
    			}
    		}
    		existing.getSites().addAll(toBeAdded);
    	}
    	
    	Glycoprotein saved = glycoproteinRepository.save(existing);
    	GlycoproteinView giv = new GlycoproteinView(saved);
    	return new ResponseEntity<>(new SuccessResponse<GlycoproteinView>(giv, "glycoprotein has been updated"), HttpStatus.OK);
    	
    }
    
    @Operation(summary = "Add glycans from file", security = { @SecurityRequirement(name = "bearer-key") })
    @PostMapping("/addglycanfromfile")
    public ResponseEntity<SuccessResponse<Boolean>> addGlycansFromFile(
    		@Parameter(required=true, name="file", description="details of the uploded file") 
	        @RequestBody
    		FileWrapper fileWrapper, 
    		@Parameter(required=true, name="filetype", description="type of the file", schema = @Schema(type = "string",
    		allowableValues= {"GWS", "WURCS", "EXCEL"})) 
	        @RequestParam(required=true, value="filetype") String fileType,
	        @RequestParam(required=false, value="tag") String tag) {
    	
    	boolean glytoucanId = false;
    	if (fileType != null && fileType.equalsIgnoreCase("Excel")) {
    		fileType = "WURCS";
    		glytoucanId = true;
    	}
    	
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
                result.setType(CollectionType.GLYCAN);
                result.setErrors(new ArrayList<>());
                result.setGlycans(new ArrayList<>());
                result.setGlycoproteins(new ArrayList<>());
                BatchUploadEntity saved = uploadRepository.save(result);
                // keep the original file in the uploads directory
                File uploadFolder = new File (uploadDir + File.separator + saved.getId());
                if (!uploadFolder.exists()) {
                	uploadFolder.mkdirs();
                }
                File newFile = new File (uploadFolder + File.separator + fileWrapper.getOriginalName());
                boolean success = file.renameTo(newFile);
                if (!success) {
                	logger.error("Could not store the original file");
                }
                try {    
                    CompletableFuture<SuccessResponse<BatchUploadEntity>> response = null;
                    
                    // process the file and add the glycans 
                    switch (format) {
                    case GWS:
                    	response = batchUploadService.addGlycanFromTextFile(fileContent, saved, user, format, ";", tag);
                    	break;
                    case WURCS:
                    	if (glytoucanId) {
                    		response = batchUploadService.addGlycanFromExcelFile(newFile, saved, user, fileWrapper.getExcelParameters(), tag);
                    	} else {
                    		response = batchUploadService.addGlycanFromTextFile(fileContent, saved, user, format, "\\n", tag);
                    	}
                    	break;
					default:
						break;
                    }
                    
                    response.whenComplete((resp, e) -> {
                    	if (e != null) {
                            logger.error(e.getMessage(), e);
                            saved.setStatus(UploadStatus.ERROR);
                            if (e.getCause() instanceof BatchUploadException) {
                            	if (saved.getErrors() == null) {
                        			saved.setErrors(new ArrayList<>());
                            	}
                            	for (UploadErrorEntity error: ((BatchUploadException)e.getCause()).getErrors()) {
                            		saved.getErrors().add(error);
                            		error.setUpload(saved);
                            	}
                            //	result.setErrors(((BatchUploadException)e.getCause()).getErrors());
                    		} else {
                    			if (saved.getErrors() == null) {
                    				saved.setErrors(new ArrayList<>());
                    			}
                    			UploadErrorEntity error = new UploadErrorEntity(null, e.getCause().getMessage(), null);
                    			error.setUpload(saved);
                    			saved.getErrors().add(error);
                    		}
                            if (saved.getGlycans() == null) {
                            	saved.setGlycans(new ArrayList<>());
                            }
                            if (saved.getGlycoproteins() == null) {
                            	saved.setGlycoproteins(new ArrayList<>());
                            }
                            try {
                            	uploadRepository.save(saved);
                            } catch (Exception ex) {
                            	logger.error("could not set status to ERROR for upload " + saved.getId(), e);
                            	ErrorReportEntity error = new ErrorReportEntity();
                    			error.setMessage("Error occurred setting the status of batch glycan upload after the process is finished with an error");
                    			StringWriter stringWriter = new StringWriter();
                    	    	PrintWriter printWriter = new PrintWriter(stringWriter);
                    	    	ex.printStackTrace(printWriter);
                    	    	error.setDetails(stringWriter.toString().substring(0, 3900));
                    	    	error.setTicketLabel("bug");
                    			error.setDateReported(new Date());
                            	errorReportingService.reportError(error);
                            }
                            
                        } else {
                        	BatchUploadEntity upload = (BatchUploadEntity) resp.getData();
                        	int count = 0;
                        	for (GlycanInFile g: upload.getGlycans()) {
                        		if (!g.getIsNew()) {
                        			count++;
                        		}
                        	}
                        	saved.setExistingCount(count);
                            saved.setStatus(UploadStatus.DONE);    
                            if (saved.getErrors() == null) saved.setErrors(new ArrayList<>());
                            if (saved.getGlycoproteins() == null) {
                            	saved.setGlycoproteins(new ArrayList<>());
                            }
                            if (saved.getGlycans() == null) {
                            	saved.setGlycans(new ArrayList<>());
                            }
                            try {
                            	uploadRepository.save(saved);
                            } catch (Exception ex) {
                            	logger.error("could not set status to DONE for upload " + saved.getId(), e);
                            	ErrorReportEntity error = new ErrorReportEntity();
                    			error.setMessage("Error occurred setting the status of batch glycan upload after the process is finished");
                    			StringWriter stringWriter = new StringWriter();
                    	    	PrintWriter printWriter = new PrintWriter(stringWriter);
                    	    	ex.printStackTrace(printWriter);
                    	    	error.setDetails(stringWriter.toString().substring(0, 3900));
                    	    	error.setTicketLabel("bug");
                    			error.setDateReported(new Date());
                            	errorReportingService.reportError(error);
                            }
                        }                       
                    });
                    response.get(100, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                	synchronized (this) {
                		Optional<BatchUploadEntity> reload = uploadRepository.findById(saved.getId());
                		if (reload.isPresent()) {
	                        if (saved.getErrors() != null && !saved.getErrors().isEmpty()) {
	                        	BatchUploadEntity existing = reload.get();
	                        	existing.setStatus(UploadStatus.ERROR);
	                        	if (existing.getErrors() == null) {
	                        		existing.setErrors(new ArrayList<>());
	                        	}
	                        	for (UploadErrorEntity err: saved.getErrors()) {
	                        		existing.getErrors().add(err);
	                        	}
	                        	uploadRepository.save(existing);
	                        } 
                		}
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
    	return new ResponseEntity<>(new SuccessResponse<Boolean>(true, "glycan added"), HttpStatus.OK); 
    }
    
    @Operation(summary = "Add glycoproteins from file", security = { @SecurityRequirement(name = "bearer-key") })
    @PostMapping("/addglycoproteinfromfile")
    public ResponseEntity<SuccessResponse<Boolean>> addGlycoproteinsFromFile(
    		@Parameter(required=true, name="file", description="details of the uploded file") 
	        @RequestBody
    		FileWrapper fileWrapper, 
    		@Parameter(required=true, name="filetype", description="type of the file", schema = @Schema(type = "string",
    		allowableValues= {"BYONIC, EXCEL"})) 
	        @RequestParam(required=true, value="filetype") String fileType,
	        @RequestParam(required=false, value="tag") String tag,
	        @Parameter(required=false, description="way to handle multiple glycans")
			@RequestParam (required=false, value="glycanorder", defaultValue="ALTERNATIVE")
			MultipleGlycanOrder multipleGlycanOrder,
			@Parameter(required=false, name="compositiontype", description="format for the compositions", schema = @Schema(type = "string",
    		allowableValues= {"BYONIC", "COMPACT"})) 
    		@RequestParam(required=false, value="compositiontype")
    		CompositionType compType) {
    	
    	
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
            BatchUploadEntity result = new BatchUploadEntity();
            result.setStartDate(new Date());
            result.setStatus(UploadStatus.PROCESSING);
            result.setUser(user);
            result.setFilename(fileWrapper.getOriginalName());
            result.setFormat(fileType);
            result.setType(CollectionType.GLYCOPROTEIN);
            result.setErrors(new ArrayList<>());
            result.setGlycans(new ArrayList<>());
            result.setGlycoproteins(new ArrayList<>());
            BatchUploadEntity saved = uploadRepository.save(result);
            // keep the original file in the uploads directory
            File uploadFolder = new File (uploadDir + File.separator + saved.getId());
            if (!uploadFolder.exists()) {
            	uploadFolder.mkdirs();
            }
            File newFile = new File (uploadFolder + File.separator + fileWrapper.getOriginalName());
            boolean success = file.renameTo(newFile);
            if (!success) {
            	logger.error("Could not store the original file");
            }
            addGlycoproteinsFromFile(this, newFile, saved, fileType, tag, multipleGlycanOrder, compType,
    			user, uploadRepository, batchUploadJobRepository, batchUploadService);
            return new ResponseEntity<>(new SuccessResponse<Boolean>(true, "batch upload finished"), HttpStatus.OK); 
        }
    }
    
    public static void rerunAddGlycoproteinsFromFile(ScheduledTasksService scheduledTasksService, File file, Long uploadId,
			String fileType, String tag, MultipleGlycanOrder orderParam, CompositionType compositionType, Long userId,
			BatchUploadRepository uploadRepository, BatchUploadJobRepository batchUploadJobRepository,
			AsyncService batchUploadService, UserRepository userRepository) {
    	
    	Optional<UserEntity> user = userRepository.findById(userId);
 		Optional<BatchUploadEntity> batch = uploadRepository.findById(uploadId);
 		
		if (batch != null && user != null) {
			BatchUploadEntity result = batch.get();
			result.setStatus(UploadStatus.PROCESSING);
			BatchUploadEntity saved = uploadRepository.save(result);
			addGlycoproteinsFromFile(scheduledTasksService, file, saved, fileType, tag, orderParam, compositionType, user.get(), 
					uploadRepository, batchUploadJobRepository, batchUploadService);
		}
		
	}
    
    public static void addGlycoproteinsFromFile (Object instance, File newFile, BatchUploadEntity result, String fileType, 
    		String tag, MultipleGlycanOrder multipleGlycanOrder, CompositionType compType,
    		UserEntity user, BatchUploadRepository uploadRepository, 
    		BatchUploadJobRepository batchUploadJobRepository, AsyncService batchUploadService) {
    	
        try {    
            CompletableFuture<SuccessResponse<BatchUploadEntity>> response = null;
            if (fileType.equalsIgnoreCase("byonic")) {
            	response = batchUploadService.addGlycoproteinFromByonicFile(newFile, result, user, ",", tag, multipleGlycanOrder);
            } else if (fileType.equalsIgnoreCase("excel")) {
            	response = batchUploadService.addGlycoproteinFromExcelFile(newFile, result, user, tag, compType);
            }
            
            response.whenComplete((resp, e) -> {
            	if (e != null) {
                    logger.error(e.getMessage(), e);
                    result.setStatus(UploadStatus.ERROR);
                    if (e.getCause() instanceof BatchUploadException) {
                    	if (result.getErrors() == null) {
                			result.setErrors(new ArrayList<>());
                    	}
                    	for (UploadErrorEntity error: ((BatchUploadException)e.getCause()).getErrors()) {
                    		result.getErrors().add(error);
                    		error.setUpload(result);
                    	}
                    	//result.setErrors(((BatchUploadException)e.getCause()).getErrors());
            		} else {
            			if (result.getErrors() == null) {
            				result.setErrors(new ArrayList<>());
            			}
            			UploadErrorEntity error = new UploadErrorEntity(null, e.getCause().getMessage(), null);
            			error.setUpload(result);
            			result.getErrors().add(error);
            		}
                    if (result.getGlycans() == null) {
                    	result.setGlycans(new ArrayList<>());
                    }
                    if (result.getGlycoproteins() == null) {
                    	result.setGlycoproteins(new ArrayList<>());
                    }
                    uploadRepository.save(result);
                    
                } else {
                	BatchUploadEntity upload = (BatchUploadEntity) resp.getData();
                	if (upload.getStatus() == UploadStatus.WAITING) {
                		// save the job and return
                		if (result.getErrors() == null) {
                			result.setErrors(new ArrayList<>());
                		}
                        if (result.getGlycans() == null) {
                        	result.setGlycans(new ArrayList<>());
                        }
                        if (result.getGlycoproteins() == null) {
                        	result.setGlycoproteins(new ArrayList<>());
                        }
                		uploadRepository.save(result);
                		List<BatchUploadJob> batchUploadJobs = batchUploadJobRepository.findAllByUpload(upload);
                		if (batchUploadJobs.isEmpty()) {
	                		BatchUploadJob job = new BatchUploadJob();
	                		job.setUpload(upload);
	                		job.setTag(tag);
	                		job.setOrderParam(multipleGlycanOrder);
	                		job.setCompType(compType);
	                		job.setUser(user);
	                		job.setFileType(fileType);
	                		job.setLastRun(new Date());
	                		batchUploadJobRepository.save(job); 
                		} else {
                			for (BatchUploadJob job: batchUploadJobs) {
                				job.setLastRun(new Date());
                				batchUploadJobRepository.save(job);
                			}
                		}
                	} else {
                    	int count = 0;
                    	for (GlycoproteinInFile g: upload.getGlycoproteins()) {
                    		if (!g.getIsNew()) {
                    			count++;
                    		}
                    	}
                    	result.setExistingCount(count);
                        result.setStatus(UploadStatus.DONE);    
                        if (result.getErrors() == null) result.setErrors(new ArrayList<>());
                        if (result.getGlycans() == null) {
                        	result.setGlycans(new ArrayList<>());
                        }
                        if (result.getGlycoproteins() == null) {
                        	result.setGlycoproteins(new ArrayList<>());
                        }
                        uploadRepository.save(result);
                	}
                }                       
            });
            response.get(1000, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
        	synchronized (result) {
                if (result.getErrors() != null && !result.getErrors().isEmpty()) {
                    result.setStatus(UploadStatus.ERROR);
                } else if (result.getErrors() == null){
                	result.setErrors(new ArrayList<>());
                }
                if (result.getGlycans() == null) {
                	result.setGlycans(new ArrayList<>());
                }
                if (result.getGlycoproteins() == null) {
                	result.setGlycoproteins(new ArrayList<>());
                }
                uploadRepository.save(result);
            }
        } catch (InterruptedException e1) {
			logger.error("batch upload is interrupted", e1);
		} catch (ExecutionException e1) {
			logger.error("batch upload is interrupted", e1);
		}
    }
    
    @Operation(summary = "Export collection", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/downloadcollection/{collectionId}")
	public ResponseEntity<Resource> exportCollection(
			@Parameter(required=true, description="id of the collection to be retrieved") 
			@PathVariable("collectionId") Long collectionId) {
    	// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        
        Collection collection = collectionRepository.findByCollectionIdAndUser(collectionId, user);
        if (collection == null) {
        	throw new IllegalArgumentException ("Could not find the given collection " + collectionId + " for the user");
        }
        
        try {
	        String jsonString = new ObjectMapper().writeValueAsString(toDTO(collection));
	        String filePath = collection.getName()+".json";
	        File newFile = new File(filePath);
	        FileWriter fileWriter = new FileWriter(newFile);
	        fileWriter.write(jsonString);
	        fileWriter.close();
	        return FileController.download(newFile, filePath, null);
        } catch (IOException e) {
        	throw new IllegalArgumentException ("Could not export given collection " + collectionId + " for the user", e);
        }
    }
    
    @Operation(summary = "Import collection", security = { @SecurityRequirement(name = "bearer-key") })
    @PostMapping("/importcollection")
	public ResponseEntity<SuccessResponse<Boolean>> importCollection(
			@Parameter(required=true, name="file", description="details of the uploded file") 
			@RequestBody
			FileWrapper fileWrapper) {
    	// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        
        String fileFolder = uploadDir;
        if (fileWrapper.getFileFolder() != null && !fileWrapper.getFileFolder().isEmpty())
            fileFolder = fileWrapper.getFileFolder();
        File file = new File (fileFolder, fileWrapper.getIdentifier());
        if (!file.exists()) {
            throw new IllegalArgumentException("File is not acceptable");
        }
        
        ObjectMapper mapper = new ObjectMapper();
        try {
            CollectionDTO collection = mapper.readValue(file, CollectionDTO.class);
            Collection c = fromDTO(collection, user);
            List<Collection> existing = collectionRepository.findAllByNameAndUser(c.getName(), user);
            if (existing != null && existing.size() > 0) {
            	// there is already a collection with the given name
            	// append something else to make it unique
            	c.setName(c.getName() + "-" + fileWrapper.getOriginalName() + "-" +  new Date());
            }
            // add glycans and glycoproteins first
            if (c.getGlycans() != null && !c.getGlycans().isEmpty()) {
            	for (GlycanInCollection gic: c.getGlycans()) {
            		// check if the glycan already exists
            		Glycan glycan = gic.getGlycan();
            		if (glycan != null && glycan.getGlytoucanID() != null) {
            			List<Glycan> eg = glycanRepository.findByGlytoucanIDIgnoreCaseAndUser(glycan.getGlytoucanID(), user);
            			if (eg != null && !eg.isEmpty()) {
            				// use the same glycan
            				gic.setGlycan(eg.get(0));
            				continue;
            			}
            		}
            		if (glycan.getTags() != null) {
	            		List<GlycanTag> newTagList = new ArrayList<>();
	            		for (GlycanTag tag: glycan.getTags()) {
		            		GlycanTag et = glycanTagRepository.findByUserAndLabel(user, tag.getLabel());
		        			if (et == null) {
		        				et = glycanTagRepository.save(tag);	
		        			}
		        			newTagList.add(et);
	            		}
	            		glycan.setTags(newTagList);
            		}
            		Glycan saved = glycanRepository.save(gic.getGlycan());
            		gic.setGlycan(saved);
            	}
            }
            if (c.getGlycoproteins() != null && !c.getGlycoproteins().isEmpty()) {
            	for (GlycoproteinInCollection gic: c.getGlycoproteins()) {
            		for (Site s: gic.getGlycoprotein().getSites()) {
            			for (GlycanInSite gis: s.getGlycans()) {
            				Glycan glycan = gis.getGlycan();
                    		if (glycan != null && glycan.getGlytoucanID() != null) {
                    			List<Glycan> eg = glycanRepository.findByGlytoucanIDIgnoreCaseAndUser(glycan.getGlytoucanID(), user);
                    			if (eg != null && !eg.isEmpty()) {
                    				// use the same glycan
                    				gis.setGlycan(eg.get(0));
                    				continue;
                    			}
                    		}
                    		if (glycan.getTags() != null) {
        	            		List<GlycanTag> newTagList = new ArrayList<>();
        	            		for (GlycanTag tag: glycan.getTags()) {
        		            		GlycanTag et = glycanTagRepository.findByUserAndLabel(user, tag.getLabel());
        		        			if (et == null) {
        		        				et = glycanTagRepository.save(tag);	
        		        			}
        		        			newTagList.add(et);
        	            		}
        	            		glycan.setTags(newTagList);
                    		}
                    		Glycan saved = glycanRepository.save(gis.getGlycan());
                    		gis.setGlycan(saved);
            			}
            		}
            		// if the glycoprotein has a name, check if it is a duplicate
            		if (gic.getGlycoprotein().getName() != null) {
            			List<Glycoprotein> egp = glycoproteinRepository.findAllByNameAndUser(gic.getGlycoprotein().getName(), user);
            			if (egp.size() > 0) {
            				// duplicate
            				gic.getGlycoprotein().setName (gic.getGlycoprotein().getName() + "-" + fileWrapper.getOriginalName() + "-" +  new Date());
            			}
            		}
            		if (gic.getGlycoprotein().getTags() != null) {
	            		List<GlycanTag> newTagList = new ArrayList<>();
	            		for (GlycanTag tag: gic.getGlycoprotein().getTags()) {
		            		GlycanTag et = glycanTagRepository.findByUserAndLabel(user, tag.getLabel());
		        			if (et == null) {
		        				et = glycanTagRepository.save(tag);	
		        			}
		        			newTagList.add(et);
	            		}
	            		gic.getGlycoprotein().setTags(newTagList);
            		}
            		Glycoprotein saved = glycoproteinRepository.save(gic.getGlycoprotein());
            		gic.setGlycoprotein(saved);
            	}
            }
            List<CollectionTag> newTagList = new ArrayList<>();
            if (collection.getTags() != null) {
	    		for (CollectionTag tag: collection.getTags()) {
	        		CollectionTag et = collectionTagRepository.findByUserAndLabel(user, tag.getLabel());
	    			if (et == null) {
	    				et = collectionTagRepository.save(tag);	
	    			}
	    			newTagList.add(et);
	    		}
	    		collection.setTags(newTagList);
            }
            // then add the collection
            collectionRepository.save(c);
            return new ResponseEntity<>(new SuccessResponse<Boolean>(true, "collection is imported from the given file"), HttpStatus.OK); 
            
        } catch (IOException e) {
        	throw new IllegalArgumentException ("Could not import collection from the given file", e);
        }
    }
    
    public Collection fromDTO (CollectionDTO dto, UserEntity user) {
    	Collection collection = new Collection();
    	collection.setUser(user);
    	collection.setName(dto.getName());
    	collection.setDescription(dto.getDescription());
    	collection.setMetadata(dto.getMetadata());   // clear the ids
    	for (Metadata m: collection.getMetadata()) {
    		m.setMetadataId(null);
    		m.setCollection(collection);
    	}
    	collection.setTags(dto.getTags());
    	if (collection.getTags() != null) {
	    	for (CollectionTag t: collection.getTags()) {
	    		t.setTagId(null);
	    		t.setUser(user);
	    	}
    	}
    	collection.setType(dto.getType());
    	
    	collection.setGlycans(dto.getGlycans().stream()
    	        .map(gc -> fromGlycanDTO(gc, collection))
    	        .collect(Collectors.toList()));
    	
    	collection.setGlycoproteins(dto.getGlycoproteins().stream()
    	        .map(gp -> fromGlycoproteinDTO(gp, collection))
    	        .collect(Collectors.toList()));
    	
    	return collection;
    }
    
    private GlycoproteinInCollection fromGlycoproteinDTO(GlycoproteinDTO dto, Collection collection) {
    	GlycoproteinInCollection gic = new GlycoproteinInCollection();
    	gic.setDateAdded(dto.getDateAdded());
    	gic.setCollection(collection);
    	Glycoprotein glycoprotein = new Glycoprotein();
    	gic.setGlycoprotein(glycoprotein);
    	
    	glycoprotein.setDateCreated(dto.getDateCreated());
    	glycoprotein.setDescription(dto.getDescription());
    	glycoprotein.setGeneSymbol(dto.getGeneSymbol());
    	glycoprotein.setName(dto.getName());
    	glycoprotein.setProteinName(dto.getProteinName());
    	glycoprotein.setSequence(dto.getSequence());
    	glycoprotein.setSequenceVersion(dto.getSequenceVersion());
    	glycoprotein.setUniprotId(dto.getUniprotId());
    	glycoprotein.setTags(dto.getTags());
    	glycoprotein.setUser(collection.getUser());
    	if (glycoprotein.getTags() != null) {
	    	for (GlycanTag t: glycoprotein.getTags()) {
	    		t.setTagId(null);
	    		t.setUser(collection.getUser());
	    	}
    	}
		glycoprotein.setSites(dto.getSites().stream()
		        .map(gs -> fromSiteDTO(gs, glycoprotein, collection.getUser()))
		        .collect(Collectors.toList()));
		return gic;
	}

	private Site fromSiteDTO(SiteDTO dto, Glycoprotein gp, UserEntity user) {
		
		Site site = new Site();
		site.setPosition(dto.getPosition());
		site.setPositionString(dto.getPositionString());
		site.setType(dto.getType());
		site.setGlycoprotein(gp);
		site.setGlycans(dto.getGlycans().stream()
		        .map(g -> fromGlycanInSiteDTO(g, site, user))
		        .collect(Collectors.toList()));
		return site;
	}

	private GlycanInSite fromGlycanInSiteDTO(GlycanInSiteDTO dto, Site site, UserEntity user) {
		GlycanInSite gis = new GlycanInSite();
		gis.setGlycosylationSubType(dto.getGlycosylationSubType());
		gis.setGlycosylationType(dto.getGlycosylationType());
		gis.setType(dto.getType());
		if (dto.getGlycan() != null) {
			gis.setGlycan(fromGlycanDTO(dto.getGlycan(), user));
		}
		gis.setSite(site);
		return gis;
	}

	public GlycanInCollection fromGlycanDTO (GlycanDTO dto, Collection collection) {
		GlycanInCollection gic = new GlycanInCollection();
		gic.setDateAdded(dto.getDateAdded());
		gic.setGlycan(fromGlycanDTO(dto, collection.getUser()));
		gic.setCollection(collection);
		return gic;
    }
	
	public Glycan fromGlycanDTO (GlycanDTO dto, UserEntity user) {
		Glycan glycan = new Glycan();
		glycan.setDateCreated(dto.getDateCreated());
		glycan.setDateCreated(dto.getDateCreated());
		glycan.setGlycoCT(dto.getGlycoCT());
		glycan.setGlytoucanHash(dto.getGlytoucanHash());
		glycan.setGlytoucanID(dto.getGlytoucanID());
		glycan.setGws(dto.getGws());
		glycan.setWurcs(dto.getWurcs());
		glycan.setMass(dto.getMass());
		glycan.setTags(dto.getTags());
		glycan.setUser(user);
		if (glycan.getTags() != null) {
	    	for (GlycanTag t: glycan.getTags()) {
	    		t.setTagId(null);
	    		t.setUser(user);
	    	}
    	}
		return glycan;
	}

	public CollectionDTO toDTO(Collection collection) {
	    CollectionDTO dto = new CollectionDTO();
	    dto.setName(collection.getName());
	    dto.setDescription(collection.getDescription());
	    dto.setType(collection.getType());
	    dto.setMetadata(new ArrayList<>(collection.getMetadata()));
	    dto.setTags(new ArrayList<>(collection.getTags()));
	    dto.setGlycans(collection.getGlycans().stream()
	        .map(gc -> toGlycanDTO(gc.getGlycan(), gc.getDateAdded()))
	        .collect(Collectors.toList()));
	
	    dto.setGlycoproteins(collection.getGlycoproteins().stream()
	        .map(gp -> toGlycoproteinDTO(gp.getGlycoprotein(), gp.getDateAdded()))
	        .collect(Collectors.toList()));
	
	    return dto;
	}

    
    private GlycoproteinDTO toGlycoproteinDTO(Glycoprotein glycoprotein, Date dateAdded) {
		GlycoproteinDTO dto = new GlycoproteinDTO();
		dto.setDateAdded(dateAdded);
		dto.setDateCreated(glycoprotein.getDateCreated());
		dto.setDescription(glycoprotein.getDescription());
		dto.setGeneSymbol(glycoprotein.getGeneSymbol());
		dto.setName(glycoprotein.getName());
		dto.setProteinName(glycoprotein.getProteinName());
		dto.setSequence(glycoprotein.getSequence());
		dto.setSequenceVersion(glycoprotein.getSequenceVersion());
		dto.setUniprotId(glycoprotein.getUniprotId());
		dto.setTags(glycoprotein.getTags());
		
		dto.setSites(glycoprotein.getSites().stream()
		        .map(gs -> toSiteDTO(gs))
		        .collect(Collectors.toList()));
		
		return dto;
	}

	private SiteDTO toSiteDTO(Site gs) {
		SiteDTO dto = new SiteDTO();
		dto.setPosition(gs.getPosition());
		dto.setPositionString(gs.getPositionString());
		dto.setType(gs.getType());
		
		dto.setGlycans(gs.getGlycans().stream()
		        .map(g -> toGlycanInSiteDTO(g))
		        .collect(Collectors.toList()));
		return dto;
	}

	private GlycanInSiteDTO toGlycanInSiteDTO(GlycanInSite g) {
		GlycanInSiteDTO dto = new GlycanInSiteDTO();
		dto.setGlycosylationSubType(g.getGlycosylationSubType());
		dto.setGlycosylationType(g.getGlycosylationType());
		dto.setType(g.getType());
		if (g.getGlycan() != null) {
			dto.setGlycan(toGlycanDTO(g.getGlycan(), null));
		}
		return dto;
	}

	private GlycanDTO toGlycanDTO(Glycan glycan, Date dateAdded) {
		GlycanDTO dto = new GlycanDTO();
		dto.setDateAdded(dateAdded);
		dto.setDateCreated(glycan.getDateCreated());
		dto.setGlycoCT(glycan.getGlycoCT());
		dto.setGlytoucanHash(glycan.getGlytoucanHash());
		dto.setGlytoucanID(glycan.getGlytoucanID());
		dto.setGws(glycan.getGws());
		dto.setWurcs(glycan.getWurcs());
		dto.setMass(glycan.getMass());
		dto.setTags(new ArrayList<>(glycan.getTags()));
		return dto;
	}

	@Operation(summary = "Download glycans", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/downloadglycans")
	public ResponseEntity<Resource> downloadGlycans (
			@Parameter(required=true, name="filetype", description="type of the file", schema = @Schema(type = "string",
    		allowableValues= {"GWS","EXCEL"}))
	        @RequestParam(required=true, value="filetype")
			GlycanFileFormat format,
			@Parameter(required=false, name="status", description="status filter")
			@RequestParam(required=false, value="status")
			Optional<RegistrationStatus> status,
			@Parameter(required=false, name="tag", description="tag filter")
			@RequestParam(required=false, value="tag")
			Optional<String> tag) {
    	// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        
        Page<Glycan> allGlycans = glycanRepository.findAllByUser(user, Pageable.unpaged());
        return downloadGlycans(allGlycans.getContent(), format, status, tag);
    }
    
    @Operation(summary = "Download glycans in a collection", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/downloadcollectionglycans")
	public ResponseEntity<Resource> downloadCollectionGlycans (
			@Parameter(required=true, name="filetype", description="type of the file", schema = @Schema(type = "string",
    		allowableValues= {"GWS","EXCEL"}))
	        @RequestParam(required=true, value="filetype")
			GlycanFileFormat format,
			@Parameter(required=false, name="status", description="status filter")
			@RequestParam(required=false, value="status")
			Optional<RegistrationStatus> status,
			@Parameter(required=false, name="tag", description="tag filter")
			@RequestParam(required=false, value="tag")
			Optional<String> tag,
			@Parameter(required=true, name="collectionid", description="collection whose glycans to be downloaded")
			@RequestParam(required=true, value="collectionid")
			Long collectionId
			) {
    	// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        
        Collection collection = collectionRepository.findByCollectionIdAndUser(collectionId, user);
        List<Glycan> glycans = new ArrayList<>();
        if (collection != null && collection.getGlycans() != null) {
        	for (GlycanInCollection g: collection.getGlycans()) {
        		glycans.add(g.getGlycan());
        	}
        }
        
        return downloadGlycans(glycans, format, status, tag);
    }
    
    ResponseEntity<Resource> downloadGlycans (List<Glycan> glycans, GlycanFileFormat format, 
    		Optional<RegistrationStatus> status,
    		Optional<String> tag) {
        StringBuffer fileContent = new StringBuffer();
        
        String filename = "glycanexport";
		String ext = (format == GlycanFileFormat.EXCEL ? ".xlsx" : ".gws");
		File newFile = new File (uploadDir + File.separator + filename + System.currentTimeMillis() + ext);
        
        TableReport tableReport = new TableReport();
		TableReportDetail report = new TableReportDetail();
		
		List<String[]> rows = new ArrayList<>();
    	Map<String, byte[]> cartoons = new HashMap<>();
    	
    	if (format == GlycanFileFormat.EXCEL) {
	    	// add header row
			String[] row = new String[6];
			row[0] = "GlyToucan ID";
			row[1] = "Status";
			row[2] = "Image";
			row[3] = "Mass";
			row[4] = "# of Collections";
			row[5] = "Information";
			
			rows.add(row);
	    }
		
		for (Glycan glycan: glycans) {
        	if (status.isPresent()) {
        		if (status.get() != glycan.getStatus()) {
        			continue;
        		}
        	}
        	
        	if (tag.isPresent()) {
        		if (!glycan.hasTag(tag.get())) {
        			continue;
        		}
        	}
		
	        switch (format) {
	        case GWS:
	        
	        	if (glycan.getGws() != null) {
	        		fileContent.append (glycan.getGws());
	        		fileContent.append(";");
	        	}
	        	else {
	        		// need to generate GWS, and add errors if it cannot be generated
	        		if (glycan.getGlycoCT() != null) {
	        			try {
	                        org.eurocarbdb.application.glycanbuilder.Glycan glycanObject = 
	                                org.eurocarbdb.application.glycanbuilder.Glycan.fromGlycoCTCondensed(glycan.getGlycoCT().trim());
	                        if (glycanObject == null) {
		        				report.addError("Cannot convert to GWS sequence");
		        			} else {
		        				fileContent.append(glycanObject.toString() + ";");
		        			}
	                    } catch (Exception e) {
	                    	// cannot get GWS string
	        				report.addError("Cannot convert to GWS sequence. Reason: " + e.getMessage());
	                    }
	        			
	        		} else if (glycan.getWurcs() != null) {
	        			try {
		        			WURCS2Parser t_wurcsparser = new WURCS2Parser();
		        	        MassOptions massOptions = new MassOptions();
		        	        massOptions.setDerivatization(MassOptions.NO_DERIVATIZATION);
		        	        massOptions.setIsotope(MassOptions.ISOTOPE_MONO);
		        	        massOptions.ION_CLOUD = new IonCloud();
		        	        massOptions.NEUTRAL_EXCHANGES = new IonCloud();
		        	        ResidueType m_residueFreeEnd = ResidueDictionary.findResidueType("freeEnd");
		        	        massOptions.setReducingEndType(m_residueFreeEnd);
		        			org.eurocarbdb.application.glycanbuilder.Glycan glycanObject = t_wurcsparser.readGlycan(glycan.getWurcs(), massOptions);
		        			if (glycanObject == null) {
		        				report.addError("Cannot convert to GWS sequence");
		        			} else {
		        				fileContent.append(glycanObject.toString() + ";");
		        			}
	        			} catch (Exception e) {
	        				// cannot get GWS string
	        				report.addError("Cannot convert to GWS sequence. Reason: " + e.getMessage());
	        			}
	        		} 
	        		
	        	}
	        
	        case EXCEL:
	    		//add glycan rows and fill in the cartoons map
	        	String[] row = new String[6];
	    		row[0] = glycan.getGlytoucanID() != null ? glycan.getGlytoucanID() : "";
	    		row[1] = glycan.getStatus().name();
	    		// retrieve/generate the cartoon
	    		try {
	                glycan.setCartoon(
	                		DataController.getImageForGlycan(
	                				imageLocation, glycan.getGlycanId()));
				} catch (DataNotFoundException e) {
					// do nothing, warning will be added later
				}
	    		if (glycan.getCartoon() != null) {
					row[2] = "IMAGE" + glycan.getGlycanId();
					cartoons.put ("IMAGE" + glycan.getGlycanId(), glycan.getCartoon());
				} else {
					// warning
					report.addWarning("Glycan " + glycan.getGlycanId() + " does not have a cartoon. Column is left empty");
					row[2] = "";
				}
	    		row[3] = glycan.getMass()+"";
	    		row[4] = glycan.getGlycanCollections().size() + "";
	    		row[5] = glycan.getError() != null ? "Glycan is submitted to Glytoucan on " + glycan.getDateCreated() + 
	    	              ". Registration failed with the following error: " + glycan.getError() : glycan.getGlytoucanHash() != null ? 
	    	                      "Glycan is submitted to Glytoucan on " + glycan.getDateCreated() + 
	    	                      ". Glytoucan assigned temporary hash value: " + glycan.getGlytoucanHash() : "";
	    		rows.add(row);
	    		
	        }
		}
		
		switch (format) {
		case GWS:
			String content = fileContent.substring(0, fileContent.length()-1);
	        
	        try {
		        FileWriter writer = new FileWriter(newFile);
		        writer.write(content);
		        writer.close();  
	        } catch (IOException e) {
	        	throw new BadRequestException ("Glycan download failed", e);
	        }
	        break;
		case EXCEL: 
			try {
    			TableController.writeToExcel(rows, cartoons, newFile, "Glycans", 1.0);
	        } catch (IOException e) {
	        	throw new BadRequestException ("Glycan download failed", e);
	        }
			break;
		}
		
		try {
        	if (report.getErrors() != null && report.getErrors().size() > 0) {
        		report.setSuccess(false);
        	} else {
        		report.setSuccess(true);
        	}
			report.setMessage("Glycan download successful");
			String reportJson = new ObjectMapper().writeValueAsString(report);
			tableReport.setReportJSON(reportJson);
			TableReport saved = reportRepository.save(tableReport);
			return FileController.download(newFile, filename+ext, saved.getReportId()+"");
        } catch (JsonProcessingException e) {
			throw new RuntimeException ("Failed to generate the error report", e);
		}
    }
    
    public static void parseAndRegisterGlycan (Glycan glycan, GlycanView g, GlycanRepository glycanRepository, ErrorReportingService errorReportingService, UserEntity user) {
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
                
                List<Glycan> existing = glycanRepository.findByWurcsIgnoreCaseAndUser(glycan.getWurcs(), user);
                if (existing != null && existing.size() > 0) {
                    throw new DuplicateException ("There is already a glycan with WURCS " + glycan.getWurcs(), null, existing.get(0));
                }
                break;
            case GWS:
                glycan.setGws(g.getSequence().trim());
                existing = glycanRepository.findByGwsIgnoreCaseAndUser(glycan.getGws(), user);
                if (existing != null && existing.size() > 0) {
                    throw new DuplicateException ("There is already a glycan with glycoworkbench sequence " + glycan.getGws(), null, existing.get(0));
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
                existing = glycanRepository.findByGlycoCTIgnoreCaseAndUser(glycan.getGlycoCT(), user);
                if (existing != null && existing.size() > 0) {
                    throw new DuplicateException ("There is already a glycan with GlycoCT " + glycan.getGlycoCT(), null, existing.get(0));
                } 
                break;
            case LINEARCODE:
            	try {
            		// initialization
                	MonosaccharideConverter t_msdb = new MonosaccharideConverter(new Config());
                	GlycoVisitorToGlycoCT t_visitorToGlycoCT = new GlycoVisitorToGlycoCT(t_msdb , GlycanNamescheme.CFG);
                	t_visitorToGlycoCT.setUseStrict(false);
            		SugarImporterCFG importer = new SugarImporterCFG();
                	sugar = importer.parse(g.getSequence().trim());
                	if (sugar == null) {
                        logger.error("Cannot get Sugar object for sequence:\n" + glycan.getGlycoCT());
                    } else {
                    	
                    	// translation successful
                    	// use on the sugar and get a sugar in the right namespace
                    	t_visitorToGlycoCT.start(sugar);
                    	sugar = t_visitorToGlycoCT.getNormalizedSugar();
                        	
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
            	} catch (Exception e) {
            		logger.error("Linearcode parsing failed for " + g.getSequence().trim(), e.getMessage());
                    throw new IllegalArgumentException ("Linearcode parsing failed. Reason " + e.getMessage());
                
            	}
            	existing = glycanRepository.findByGlycoCTIgnoreCaseAndUser(glycan.getGlycoCT(), user);
                if (existing != null && existing.size() > 0) {
                    throw new DuplicateException ("There is already a glycan with GlycoCT " + glycan.getGlycoCT(), null, existing.get(0));
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
                if (existing != null && existing.size() > 0) {
                    throw new DuplicateException ("There is already a glycan with GlycoCT " + glycan.getGlycoCT(), null, existing.get(0));
                }    
            }
            // check if the glycan has an accession number in Glytoucan
            if (glycan.getGlytoucanID() == null || glycan.getGlytoucanID().trim().isEmpty()) {
            	SequenceUtils.getWurcsAndGlytoucanID(glycan, sugar);
            }
            if (glycan.getGlytoucanID() == null || glycan.getGlytoucanID().isEmpty()) {
            	SequenceUtils.registerGlycan(glycan);
            } 
        } catch (GlycoVisitorException e) {
            throw new IllegalArgumentException(e.getMessage());
        } catch (GlytoucanAPIFailedException e) {
        	glycan.setStatus(RegistrationStatus.NOT_SUBMITTED_YET);
        	//glycan.setError("Cannot retrieve glytoucan id. Reason: " + e.getMessage());
        	// report the error through email
        	ErrorReportEntity error = new ErrorReportEntity();
			error.setMessage(e.getMessage());
			error.setDetails("Error occurred in parse and register glycan");
			error.setDateReported(new Date());
			error.setTicketLabel("GlytoucanAPI");
			errorReportingService.reportError(error);
        }          
    }
    
    public static String cleanupSequence (String a_sequence) {
        String sequence = a_sequence.trim();
        sequence = sequence.replaceAll(" ", "");
        sequence = sequence.replaceAll("\u00A0", "");
        if (sequence.endsWith("1") || sequence.endsWith("2")) {
            sequence = sequence.substring(0, sequence.length()-1);
        }
        return sequence;
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
    
    public static byte[] getImageForGlycan (String imageLocation, Long glycanId) {
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
