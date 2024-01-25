package org.glygen.tablemaker.controller;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.eurocarbdb.MolecularFramework.io.SugarImporterException;
import org.eurocarbdb.MolecularFramework.io.GlycoCT.SugarExporterGlycoCTCondensed;
import org.eurocarbdb.MolecularFramework.io.GlycoCT.SugarImporterGlycoCTCondensed;
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
import org.glycoinfo.GlycanCompositionConverter.conversion.CompositionConverter;
import org.glycoinfo.GlycanCompositionConverter.conversion.ConversionException;
import org.glycoinfo.GlycanCompositionConverter.structure.Composition;
import org.glycoinfo.GlycanCompositionConverter.utils.CompositionParseException;
import org.glycoinfo.GlycanCompositionConverter.utils.CompositionUtils;
import org.glycoinfo.GlycanCompositionConverter.utils.DictionaryException;
import org.glycoinfo.WURCSFramework.io.GlycoCT.GlycoVisitorValidationForWURCS;
import org.glycoinfo.WURCSFramework.io.GlycoCT.WURCSExporterGlycoCT;
import org.glycoinfo.WURCSFramework.util.WURCSException;
import org.glycoinfo.WURCSFramework.util.validation.WURCSValidator;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.glygen.tablemaker.exception.DataNotFoundException;
import org.glygen.tablemaker.exception.DuplicateException;
import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.dao.GlycanRepository;
import org.glygen.tablemaker.persistence.dao.UserRepository;
import org.glygen.tablemaker.persistence.glycan.Glycan;
import org.glygen.tablemaker.persistence.glycan.RegistrationStatus;
import org.glygen.tablemaker.util.FixGlycoCtUtil;
import org.glygen.tablemaker.util.GlytoucanUtil;
import org.glygen.tablemaker.view.Filter;
import org.glygen.tablemaker.view.GlycanView;
import org.glygen.tablemaker.view.Sorting;
import org.glygen.tablemaker.view.SuccessResponse;
import org.glygen.tablemaker.view.UserStatisticsView;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
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
    final private UserRepository userRepository;
    
    @Value("${spring.file.imagedirectory}")
    String imageLocation;
    
    public DataController(GlycanRepository glycanRepository, UserRepository userRepository) {
        this.glycanRepository = glycanRepository;
        this.userRepository = userRepository;
    }
    
    @Operation(summary = "Get data counts", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/statistics")
    public ResponseEntity<SuccessResponse> getStatistics() {
        UserStatisticsView stats = new UserStatisticsView();
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
        
        // TODO apply filters
        Page<Glycan> glycansInPage = glycanRepository.findAllByUser(user, PageRequest.of(start, size, Sort.by(sortOrders)));
        
        // retrieve cartoon images
        for (Glycan g: glycansInPage.getContent()) {
            try {
                g.setCartoon(getImageForGlycan(g.getGlycanId()));
            } catch (DataNotFoundException e) {
                // ignore
                logger.warn ("no image found for glycan " + g.getGlycanId());
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("glycans", glycansInPage.getContent());
        response.put("currentPage", glycansInPage.getNumber());
        response.put("totalItems", glycansInPage.getTotalElements());
        response.put("totalPages", glycansInPage.getTotalPages());
        
        return new ResponseEntity<>(new SuccessResponse(response, "glycans retrieved"), HttpStatus.OK);
    }
    
    @Operation(summary = "Delete given glycan from the user's list", security = { @SecurityRequirement(name = "bearer-key") })
    @RequestMapping(value="/delete/{glycanId}", method = RequestMethod.DELETE)
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
        //TODO need to check if the glycan appears in any collection and delete from the collections first
        glycanRepository.deleteById(glycanId);
        return new ResponseEntity<>(new SuccessResponse(glycanId, "Glycan deleted successfully"), HttpStatus.OK);
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
            Glycan existing = glycanRepository.findByGlytoucanIDIgnoreCase(g.getGlytoucanID().trim());
            if (existing != null) {
                throw new DuplicateException ("There is already a glycan with GlyTouCan ID " + g.getGlytoucanID());
            }
            // check glytoucan to see if the id is correct!
            String sequence = getSequenceFromGlytoucan(g.getGlytoucanID().trim());
            glycan.setWurcs(sequence);
            WURCS2Parser t_wurcsparser = new WURCS2Parser();
            MassOptions massOptions = new MassOptions();
            massOptions.setDerivatization(MassOptions.NO_DERIVATIZATION);
            massOptions.setIsotope(MassOptions.ISOTOPE_MONO);
            massOptions.ION_CLOUD = new IonCloud();
            massOptions.NEUTRAL_EXCHANGES = new IonCloud();
            ResidueType m_residueFreeEnd = ResidueDictionary.findResidueType("freeEnd");
            massOptions.setReducingEndType(m_residueFreeEnd);
            try {
                org.eurocarbdb.application.glycanbuilder.Glycan glycanObject = t_wurcsparser.readGlycan(glycan.getWurcs(), massOptions);
                glycan.setMass(computeMass(glycanObject));
            } catch (Exception e) {
                logger.error("could not calculate mass for wurcs sequence ", e);
                glycan.setError("Could not calculate mass. Reason: " + e.getMessage());
            }
            glycan.setStatus(RegistrationStatus.ALREADY_IN_GLYTOUCAN);
            
        } else if (g.getSequence() != null && !g.getSequence().trim().isEmpty()){ 
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
                        WURCS2Parser t_wurcsparser = new WURCS2Parser();
                        MassOptions massOptions = new MassOptions();
                        massOptions.setDerivatization(MassOptions.NO_DERIVATIZATION);
                        massOptions.setIsotope(MassOptions.ISOTOPE_MONO);
                        massOptions.ION_CLOUD = new IonCloud();
                        massOptions.NEUTRAL_EXCHANGES = new IonCloud();
                        ResidueType m_residueFreeEnd = ResidueDictionary.findResidueType("freeEnd");
                        massOptions.setReducingEndType(m_residueFreeEnd);
                        try {
                            glycanObject = t_wurcsparser.readGlycan(g.getSequence(), massOptions);
                            glycan.setMass(computeMass(glycanObject));
                        } catch (Exception e) {
                            logger.error("could not calculate mass for wurcs sequence ", e);
                            glycan.setError("Could not calculate mass. Reason: " + e.getMessage());
                        }
                    }
                    
                    Glycan existing = glycanRepository.findByWurcsIgnoreCase(glycan.getWurcs());
                    if (existing != null) {
                        throw new DuplicateException ("There is already a glycan with WURCS " + glycan.getWurcs());
                    }
                    break;
                case GWS:
                    glycan.setGws(g.getSequence().trim());
                    existing = glycanRepository.findByGwsIgnoreCase(glycan.getGws());
                    if (existing != null) {
                        throw new DuplicateException ("There is already a glycan with glyco workbench sequence " + glycan.getGws());
                    }
                    try {
                        // parse and convert to GlycoCT
                        glycanObject = org.eurocarbdb.application.glycanbuilder.Glycan.fromString(glycan.getGws());
                        String glycoCT = glycanObject.toGlycoCTCondensed();
                        glycoCT = fixGlycoCT.fixGlycoCT(glycoCT);
                        glycan.setGlycoCT(glycoCT);
                        glycan.setMass(computeMass(glycanObject));
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
                            glycan.setMass(computeMass(glycanObject));
                        }
                    } catch (Exception e) {
                        logger.error("Glycan builder parse error", e.getMessage());
                        // check to make sure GlycoCT valid without using GWB
                        SugarImporterGlycoCTCondensed importer = new SugarImporterGlycoCTCondensed();
                        try {
                            sugar = importer.parse(glycan.getGlycoCT());
                            if (sugar == null) {
                                logger.error("Cannot get Sugar object for sequence:" + glycan.getGlycoCT());
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
                    existing = glycanRepository.findByGlycoCTIgnoreCase(glycan.getGlycoCT());
                    if (existing != null) {
                        throw new DuplicateException ("There is already a glycan with GlycoCT " + glycan.getGlycoCT());
                    }    
                }
                // check if the glycan has an accession number in Glytoucan
                getWurcsAndGlytoucanID(glycan, sugar);
            } catch (GlycoVisitorException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
            
            if (glycan.getGlytoucanID() == null || glycan.getGlytoucanID().isEmpty()) {
                // if not, register
                // store the hash and update status 
                String glyToucanId = GlytoucanUtil.getInstance().registerGlycan(glycan.getWurcs());
                logger.info("Got glytoucan id after registering the glycan:" + glyToucanId);
            
                if (glyToucanId == null || glyToucanId.length() > 10) {
                    // this is new registration, hash returned
                    String glyToucanHash = glyToucanId;
                    glycan.setGlytoucanHash(glyToucanHash);
                    logger.info("got glytoucan hash, no accession number!");
                    glycan.setStatus(RegistrationStatus.NEWLY_SUBMITTED_FOR_REGISTRATION);
                } else {
                    glycan.setGlytoucanID(glyToucanId);
                    glycan.setStatus(RegistrationStatus.NEWLY_REGISTERED);
                }
            }   
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
                    WURCS2Parser t_wurcsparser = new WURCS2Parser();
                    MassOptions massOptions = new MassOptions();
                    massOptions.setDerivatization(MassOptions.NO_DERIVATIZATION);
                    massOptions.setIsotope(MassOptions.ISOTOPE_MONO);
                    massOptions.ION_CLOUD = new IonCloud();
                    massOptions.NEUTRAL_EXCHANGES = new IonCloud();
                    ResidueType m_residueFreeEnd = ResidueDictionary.findResidueType("freeEnd");
                    massOptions.setReducingEndType(m_residueFreeEnd);
                    try {
                        org.eurocarbdb.application.glycanbuilder.Glycan glycanObject = t_wurcsparser.readGlycan(g.getSequence(), massOptions);
                        glycan.setMass(computeMass(glycanObject));
                    } catch (Exception e) {
                        logger.error("could not calculate mass for wurcs sequence ", e);
                        glycan.setError("Could not calculate mass. Reason: " + e.getMessage());
                    }
                }
                getWurcsAndGlytoucanID(glycan, null);
                Glycan existing = glycanRepository.findByWurcsIgnoreCase(glycan.getWurcs());
                if (existing != null) {
                    throw new DuplicateException ("There is already a glycan with WURCS " + glycan.getWurcs());
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
    
    Double computeMass (org.eurocarbdb.application.glycanbuilder.Glycan glycanObject) {
        if (glycanObject != null) {
            MassOptions massOptions = new MassOptions();
            massOptions.setDerivatization(MassOptions.NO_DERIVATIZATION);
            massOptions.setIsotope(MassOptions.ISOTOPE_MONO);
            massOptions.ION_CLOUD = new IonCloud();
            massOptions.NEUTRAL_EXCHANGES = new IonCloud();
            ResidueType m_residueFreeEnd = ResidueDictionary.findResidueType("freeEnd");
            massOptions.setReducingEndType(m_residueFreeEnd);
            glycanObject.setMassOptions(massOptions);
            return glycanObject.computeMass();
        } 
        return null;
    }
    
    public void getWurcsAndGlytoucanID (Glycan glycan, Sugar sugar) throws GlycoVisitorException { 
        String wurcs = glycan.getWurcs();
        if (wurcs == null) {
            try {
                WURCSExporterGlycoCT exporter = new WURCSExporterGlycoCT();
                exporter.start(glycan.getGlycoCT());
                wurcs = exporter.getWURCS();
                // validate first
                WURCSValidator validator = new WURCSValidator();
                validator.start(wurcs);
                if (validator.getReport().hasError()) {
                    String errorMessage = "";
                    for (String error: validator.getReport().getErrors()) {
                        errorMessage += error + ", ";
                    }
                    errorMessage = errorMessage.substring(0, errorMessage.lastIndexOf(","));
                    throw new IllegalArgumentException ("WURCS conversion error. Reason " + errorMessage);
                } 
            } catch (WURCSException | SugarImporterException | GlycoVisitorException e) {
                logger.warn ("cannot convert sequence into Wurcs to check glytoucan", e);
                logger.info("Glycan sequence that failed: " + glycan.getGlycoCT().trim());
                String [] codes;
                if (sugar != null) {
                    // run the validator to get the detailed error messages
                    GlycoVisitorValidationForWURCS t_validationWURCS = new GlycoVisitorValidationForWURCS();
                    t_validationWURCS.start(sugar);
                    codes = t_validationWURCS.getErrors().toArray(new String[0]);
                } else {
                    codes = new String[] {"Sequence: " + glycan.getGlycoCT(), "Error Message: " + e.getMessage()};
                }
                throw new IllegalArgumentException ("WURCS conversion error. Reason " + String.join(",", codes));
            }
        } 
        String glyToucanId = GlytoucanUtil.getInstance().getAccessionNumber(wurcs);
        if (glyToucanId != null) {
            glycan.setGlytoucanID(glyToucanId);
            glycan.setStatus(RegistrationStatus.ALREADY_IN_GLYTOUCAN);
        }
    }
    
    public String getSequenceFromGlytoucan (String glytoucanId) {
        try {
            String wurcsSequence = GlytoucanUtil.getInstance().retrieveGlycan(glytoucanId.trim());
            if (wurcsSequence == null) {
                // cannot be found in Glytoucan
                throw new DataNotFoundException("Glycan with accession number " + glytoucanId + " cannot be found");
            } 
            return wurcsSequence;           
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Input: Glytoucan ID" + glytoucanId + " failed. Reason: " + e.getMessage());
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
