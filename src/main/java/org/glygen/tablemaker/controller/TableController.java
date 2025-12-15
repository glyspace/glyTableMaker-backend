package org.glygen.tablemaker.controller;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.glygen.tablemaker.exception.BadRequestException;
import org.glygen.tablemaker.exception.DataNotFoundException;
import org.glygen.tablemaker.exception.DuplicateException;
import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.dao.CollectionRepository;
import org.glygen.tablemaker.persistence.dao.TableReportRepository;
import org.glygen.tablemaker.persistence.dao.TemplateRepository;
import org.glygen.tablemaker.persistence.dao.UserRepository;
import org.glygen.tablemaker.persistence.glycan.Collection;
import org.glygen.tablemaker.persistence.glycan.CollectionType;
import org.glygen.tablemaker.persistence.glycan.GlycanInCollection;
import org.glygen.tablemaker.persistence.glycan.Metadata;
import org.glygen.tablemaker.persistence.protein.GlycanInSite;
import org.glygen.tablemaker.persistence.protein.GlycoproteinInCollection;
import org.glygen.tablemaker.persistence.protein.Site;
import org.glygen.tablemaker.persistence.protein.SitePosition;
import org.glygen.tablemaker.persistence.table.FileFormat;
import org.glygen.tablemaker.persistence.table.GlycanColumns;
import org.glygen.tablemaker.persistence.table.TableColumn;
import org.glygen.tablemaker.persistence.table.TableMakerTemplate;
import org.glygen.tablemaker.persistence.table.TableReport;
import org.glygen.tablemaker.persistence.table.TableReportDetail;
import org.glygen.tablemaker.persistence.table.TableView;
import org.glygen.tablemaker.view.SuccessResponse;
import org.grits.toolbox.glycanarray.om.util.ExcelWriterHelper;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVWriter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/table")
public class TableController {
	
	static Logger logger = org.slf4j.LoggerFactory.getLogger(TableController.class);
	
	@Value("${spring.file.uploaddirectory}")
	String uploadDir;
	
	@Value("${spring.file.imagedirectory}")
	String imageLocation;
	
	final private UserRepository userRepository;
	final private TemplateRepository templateRepository;
	final private TableReportRepository reportRepository;
	final private CollectionRepository collectionRepository;
	
	public TableController(UserRepository userRepository, TemplateRepository templateRepository, TableReportRepository reportRepository, CollectionRepository collectionRepository) {
		this.userRepository = userRepository;
		this.templateRepository = templateRepository;
		this.reportRepository = reportRepository;
		this.collectionRepository = collectionRepository;
	}
	
	@Operation(summary = "Get all templates for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/gettemplates")
    public ResponseEntity<SuccessResponse<List<TableMakerTemplate>>> getTemplates() {
		// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
		List<TableMakerTemplate> templates = templateRepository.findAllByUser(user);
		Optional<TableMakerTemplate> glygenTemplate = templateRepository.findById(1L);
		Optional<TableMakerTemplate> glycoproteinTemplate = templateRepository.findById(2L);
		if (glygenTemplate.isPresent()) templates.add(0, glygenTemplate.get());
		if (glycoproteinTemplate.isPresent()) templates.add(1, glycoproteinTemplate.get());
		
		// order columns according to the given order
		for (TableMakerTemplate template: templates) {
			ArrayList<TableColumn> columns = new ArrayList<>(template.getColumns());
			Collections.sort(columns, Comparator.comparing(TableColumn::getOrder));
			template.setColumns(columns);
		}
    	return new ResponseEntity<>(
    			new SuccessResponse<List<TableMakerTemplate>> (templates, "templates retrieved"), HttpStatus.OK);
    	
    }
	
	@Operation(summary = "Get download report", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/getreport/{reportId}")
    public ResponseEntity<SuccessResponse<TableReportDetail>> getTableReport(
    		@Parameter(required=true, description="id of the report to be retrieved") 
    		@PathVariable("reportId") Long reportId) {
		Optional<TableReport> report = reportRepository.findById(reportId);
		if (report.isPresent()) {
			TableReport r = report.get();
			if (r.getReportJSON() != null) {
				try {
					TableReportDetail detail = new ObjectMapper().readValue (r.getReportJSON(), TableReportDetail.class);
					return new ResponseEntity<>(
			    			new SuccessResponse<TableReportDetail> (detail, "report retrieved"), HttpStatus.OK);
				} catch (JsonProcessingException e) {
					throw new RuntimeException("Could not get report details");
				}
			}
			
		} 
		throw new EntityNotFoundException("Download report with the given id " + reportId + " is not found");
    	
    }
	
	@Operation(summary = "Add table maker template for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @PostMapping("/addtemplate")
    public ResponseEntity<SuccessResponse<TableMakerTemplate>> addTemplate(
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
    	return new ResponseEntity<>(new SuccessResponse<TableMakerTemplate>(saved, "template added"), HttpStatus.OK);
	}
	
	@Operation(summary = "Generate table and download", security = { @SecurityRequirement(name = "bearer-key") })
    @PostMapping("/downloadtable")
	public ResponseEntity<Resource> downloadTable (@Valid @RequestBody TableView table) {
		String filename = table.getFilename() != null ? table.getFilename() : "tablemakerexport";
		String ext = (table.getFileFormat() == FileFormat.EXCEL ? ".xlsx" : ".csv");
		File newFile = new File (uploadDir + File.separator + filename + System.currentTimeMillis() + ext);
		
		List<Collection> collectionList = new ArrayList<>();
		for (Collection c: table.getCollections()) {
			Optional<Collection> loaded = collectionRepository.findById(c.getCollectionId());
			if (loaded.isPresent()) {
				if (loaded.get().getCollections() != null && !loaded.get().getCollections().isEmpty()) {
					for (Collection child: loaded.get().getCollections()) {
						Optional<Collection> cc = collectionRepository.findById(child.getCollectionId());
						if (cc.isPresent())
							collectionList.add(cc.get());
					}
				} else {
					collectionList.add(loaded.get());
				}
			}
		}
		List<String[]> rows = new ArrayList<>();
		Map<String, byte[]> cartoons = new HashMap<>();
		
		TableReport tableReport = new TableReport();
		TableReportDetail report = new TableReportDetail();
		
		if (table.getFileFormat() == FileFormat.CSV) {
			for (TableColumn col: table.getColumns()) {
				if (col.getGlycanColumn() != null) {
					if (col.getGlycanColumn() == GlycanColumns.CARTOON) {
						//throw new BadRequestException("Cartoons cannot be written in CSV files");
						report.addError("Cartoons cannot be written in CSV files");
					}
				}
			}
		}
		
		// generate cartoons for glycans, if CARTOON column is requested
		for (TableColumn col: table.getColumns()) {
			if (col.getGlycanColumn() == GlycanColumns.CARTOON) {
				for (Collection c: collectionList) {
					if (c.getGlycans() != null) {
						for (GlycanInCollection g: c.getGlycans()) {
							try {
				               DataController.getImageForGlycan(imageLocation, g.getGlycan());
							} catch (DataNotFoundException e) {
								// do nothing, warning will be added later
							}
						}
					}
				}
			}
		}
		
		// add header row
		String[] row = new String[table.getColumns().size()];
		int column = 0;
		for (TableColumn col: table.getColumns()) {
			row[column++] = col.getName();
		}
		rows.add(row);
		for (Collection c: collectionList) {
			if (c.getType() == null || c.getType() == CollectionType.GLYCAN) {
				for (GlycanInCollection g: c.getGlycans()) {
					row = new String[table.getColumns().size()];
					rows.add(row);
					int i=0;
					for (TableColumn col: table.getColumns()) {
						if (col.getGlycanColumn() != null) {
							switch (col.getGlycanColumn()) {
							case CARTOON:
								if (g.getGlycan().getCartoon() == null && col.getDefaultValue() != null) {
									row[i] = col.getDefaultValue();
								} else {
									if (g.getGlycan().getCartoon() != null) {
										row[i] = "IMAGE" + g.getGlycan().getGlycanId();
										cartoons.put ("IMAGE" + g.getGlycan().getGlycanId(), g.getGlycan().getCartoon());
									} else {
										// warning
										report.addWarning("Glycan " + g.getGlycan().getGlycanId() + " in collection " + c.getName() + " does not have a cartoon. Column is left empty");
										row[i] = "";
									}
								}
								break;
							case GLYTOUCANID:
								if (g.getGlycan().getGlytoucanID() == null && col.getDefaultValue() != null) {
									row[i] = col.getDefaultValue();
								} else if (g.getGlycan().getGlytoucanID() != null){
									row[i] = g.getGlycan().getGlytoucanID();
								} else {
									// warning
									report.addWarning("Glycan " + g.getGlycan().getGlycanId() + " in collection " + c.getName() + " does not have a value for GlytoucanID. Column is left empty!");
									row[i] = "";
								}
								break;
							case MASS:
								if (g.getGlycan().getMass() == null && col.getDefaultValue() != null) {
									row[i] = col.getDefaultValue();
								} else if (g.getGlycan().getMass() != null){
									row[i] = g.getGlycan().getMass() + "";
								} else {
									// warning
									report.addWarning("Glycan " + g.getGlycan().getGlycanId() + " in collection " + c.getName() + " does not have a value for mass. Column is left empty!");
									row[i] = "";
								}
								break;
							default:
								row[i] = "";
								break;
							}
						} else if (col.getDatatype() != null) {
							handleDatatypeColumn(c, col, row, i, report);
						} else if (col.getDefaultValue() != null) {
							row[i] = col.getDefaultValue();
						} else {
							row[i] = "";
						}
						i++;
					}
				}
			}
			else {
				// glycoprotein collection
				for (GlycoproteinInCollection gp: c.getGlycoproteins()) {
					for (Site s: gp.getGlycoprotein().getSites()) {
						if (s.getPositionString() != null) {
	        				ObjectMapper om = new ObjectMapper();
	        				try {
								s.setPosition(om.readValue(s.getPositionString(), SitePosition.class));
							} catch (JsonProcessingException e) {
								logger.warn ("Position string is invalid: " + s.getPositionString());
							}
	        			}
						for (GlycanInSite g: s.getGlycans()) {
							row = new String[table.getColumns().size()];
							rows.add(row);
							int i=0;
							for (TableColumn col: table.getColumns()) {
								if (col.getProteinColumn() != null) { 
									switch (col.getProteinColumn()) {
									case AMINOACID:
										if ( s.getAminoAcidString().isBlank() && col.getDefaultValue() != null) {
											row[i] = col.getDefaultValue();
										} else if (!s.getAminoAcidString().isBlank()){
											row[i] = s.getAminoAcidString();
										} else {
											// warning
											report.addWarning("Protein " + gp.getGlycoprotein().getName() + " in collection " + c.getName() + " does not have a value for Aminoacid. Column is left empty!");
											row[i] = "";
										}
										break;
									case GLYCOSYLATIONSUBTYPE:
										if (g.getGlycosylationSubType() == null && col.getDefaultValue() != null) {
											row[i] = col.getDefaultValue();
										} else if (g.getGlycosylationSubType() != null){
											row[i] = g.getGlycosylationSubType();
										} else {
											// warning
											report.addWarning("Glycan " + g.getGlycan().getGlycanId() + " in protein " + gp.getGlycoprotein().getName() + " in collection " + c.getName() + " does not have a value for Glycosylation Subtype. Column is left empty!");
											row[i] = "";
										}
										break;
									case GLYCOSYLATIONTYPE:
										if (g.getGlycosylationType() == null && col.getDefaultValue() != null) {
											row[i] = col.getDefaultValue();
										} else if (g.getGlycosylationType() != null){
											row[i] = g.getGlycosylationType();
										} else {
											// warning
											report.addWarning("Glycan " + g.getGlycan().getGlycanId() + " in protein " + gp.getGlycoprotein().getName() + " in collection " + c.getName() + " does not have a value for Glycosylation Type. Column is left empty!");
											row[i] = "";
										}
										break;
									case GLYTOUCANID:
										if (g.getGlycan() != null) {
											if (g.getGlycan().getGlytoucanID() == null && col.getDefaultValue() != null) {
												row[i] = col.getDefaultValue();
											} else if (g.getGlycan().getGlytoucanID() != null){
												row[i] = g.getGlycan().getGlytoucanID();
											} else {
												// warning
												report.addWarning("Glycan " + g.getGlycan().getGlycanId() + " in protein " + gp.getGlycoprotein().getName() + " in collection " + c.getName() + " does not have a value for GlytoucanID. Column is left empty!");
												row[i] = "";
											}
										}
										break;
									case SITE:
										if ( s.getLocationString().isBlank() && col.getDefaultValue() != null) {
											row[i] = col.getDefaultValue();
										} else if (!s.getLocationString().isBlank()){
											row[i] = s.getLocationString();
										} else {
											// warning
											report.addWarning("Protein " + gp.getGlycoprotein().getName() + " in collection " + c.getName() + " does not have a value for site location. Column is left empty!");
											row[i] = "";
										}
										break;
									case UNIPROTID:
										if (gp.getGlycoprotein().getUniprotId() == null && col.getDefaultValue() != null) {
											row[i] = col.getDefaultValue();
										} else if (gp.getGlycoprotein().getUniprotId() != null) {
											row[i] = gp.getGlycoprotein().getUniprotId();
										} else {
											// warning
											report.addWarning("Protein " + gp.getGlycoprotein().getName() + " in collection " + c.getName() + " does not have a value for Uniprot ID. Column is left empty!");
											row[i] = "";
										}
										break;
									default:
										break;
									
									}
								} else if (col.getDatatype() != null) {
									handleDatatypeColumn(c, col, row, i, report);
								}
								else if (col.getDatatype() != null) {
									handleDatatypeColumn(c, col, row, i, report);
								} else if (col.getDefaultValue() != null) {
									row[i] = col.getDefaultValue();
								} else {
									row[i] = "";
								}
								i++;
							}
						}
					}
				}
			}
		}
		
		if (rows.size() == 1) {
			// no glycans found in the collections
			report.addError("There are no glycans/glycoproteins in the selected collections");
		}
		
		if (report.getErrors() == null || report.getErrors().isEmpty()) {
			try {
				// create the file
				if (table.getFileFormat() == FileFormat.EXCEL) {
					writeToExcel (rows, cartoons, newFile, "TableMaker", table.getImageScale());
				} else {
					writeToCSV(rows, newFile);
				}
				// save the report
				try {
					report.setSuccess(true);
					report.setMessage("Table creation successful");
					String reportJson = new ObjectMapper().writeValueAsString(report);
					tableReport.setReportJSON(reportJson);
					TableReport saved = reportRepository.save(tableReport);
					return FileController.download(newFile, filename+ext, saved.getReportId()+"");
				} catch (JsonProcessingException e) {
					throw new RuntimeException ("Failed to generate the report", e);
				}	
			} catch (IOException e) {
				throw new BadRequestException ("Table creation failed", e);
			}
		} else {
			// save the report
			report.setSuccess(false);
			report.setMessage("Table creation failed");
			try {
				String reportJson = new ObjectMapper().writeValueAsString(report);
				tableReport.setReportJSON(reportJson);
				TableReport saved = reportRepository.save(tableReport);
				throw new BadRequestException(saved.getReportId()+"");
			} catch (JsonProcessingException e) {
				throw new RuntimeException ("Failed to generate the report", e);
			}
			
		}
	}
	
	void handleDatatypeColumn (Collection c, TableColumn col, String[] row, int i, TableReportDetail report) {
		boolean found = false;
		for (Metadata metadata: c.getMetadata()) {
			if (metadata.getType().getDatatypeId().equals(col.getDatatype().getDatatypeId())) {
				if (col.getType() != null) {
					switch (col.getType()) {
					case ID:
						if (!metadata.getType().getNamespace().getHasId()) {
							// ERROR!
							report.addError(metadata.getType().getNamespace().getName() + " does not have value ID but \"ID\" is requested for " + col.getName() + " column.");
						} else {
							if (metadata.getValueId() == null && col.getDefaultValue() != null) {
								row[i] = col.getDefaultValue();
							} else {
								if (!found) {   // first one
									row[i] = metadata.getValueId();
								} else {
									row[i] += "|" + metadata.getValueId();
								}
							}
						}
						break;
					case URI:
						if (!metadata.getType().getNamespace().getHasUri()) {
							// ERROR!
							report.addError(metadata.getType().getNamespace().getName() + " does not have value URI but \"URI\" is requested for " + col.getName() + " column.");
						} else {
							if (metadata.getValueUri() == null && col.getDefaultValue() != null) {
								row[i] = col.getDefaultValue();
							} else {
								if (!found) {   // first one
									row[i] = metadata.getValueUri();
								} else {
									row[i] += "|" + metadata.getValueUri();
								}
							}
						}
						break;
					case VALUE:
					default:
						if (metadata.getValue() == null && col.getDefaultValue() != null) {
							row[i] = col.getDefaultValue();
						} else {
							if (!found) {   // first one
								row[i] = metadata.getValue();
							} else {
								row[i] += "|" + metadata.getValue();
							}
						}
						break;
					}
				} else {
					if (metadata.getValue() == null && col.getDefaultValue() != null) {
						row[i] = col.getDefaultValue();
					} else {
						if (!found) {   // first one
							row[i] = metadata.getValue();
						} else {
							row[i] += "|" + metadata.getValue();
						}
					}
				}
				found = true;
			}
		}
		if (!found) {
			// add a warning to the report
			// warning
			report.addWarning(c.getName() + " does not have metadata for \"" + col.getName() + "\" column. Column is left empty!");
			row[i] = "";	
		}
	}
		
	public static void writeToCSV(List<String[]> rows, File newFile) throws IOException {
		try (CSVWriter writer = new CSVWriter(new FileWriter(newFile.getPath().toString()))) {
	        writer.writeAll(rows);
	    }
	}

	public static void writeToExcel(List<String[]> rows, Map<String, byte[]> cartoons, File newFile, String sheetName, Double scale) throws IOException {
		FileOutputStream excelWriter = new FileOutputStream(newFile);
		ExcelWriterHelper helper = new ExcelWriterHelper();
		Workbook workbook = new XSSFWorkbook();
		List<Picture> m_lPictures = new ArrayList<>(); 
        
        XSSFFont font= (XSSFFont) workbook.createFont();
        font.setBold(true);
        font.setItalic(false);
        CellStyle boldStyle = workbook.createCellStyle();
        boldStyle.setFont(font);
        
        Sheet sheet = workbook.createSheet(sheetName);
        CellStyle wrapTextStyle = workbook.createCellStyle();
        wrapTextStyle.setWrapText(true);
        
        // first row is the header row
        if (rows.size() > 0) {
        	String[] headerRow = rows.get(0);
        	Row header = sheet.createRow(0);
        	int i=0;
        	for (String col: headerRow) {
        		Cell cell = header.createCell(i++);
        		cell.setCellValue(col);
        		cell.setCellStyle(boldStyle);
        	}
        	
        	for (i=1; i< rows.size(); i++) {
        		String[] row = rows.get(i);
        		Row entry = sheet.createRow(i);
        		int j=0;
        		for (String col: row) {
        			Cell cell = entry.createCell(j++);
        			if (col.startsWith ("IMAGE")) {
        				// find the cartoon 
        				String glycanId = col.substring(col.indexOf("IMAGE")+5);
        				byte[] cartoon = cartoons.get(col);
        				if (cartoon != null) {
        					InputStream is = new ByteArrayInputStream(cartoon);
        			        try {
								BufferedImage newBi = ImageIO.read(is);
								if (scale != 1.0) newBi = scale (newBi, scale);
	        				    helper.writeCellImage(workbook, sheet, i, j-1, newBi, m_lPictures);
							} catch (Exception e) {
								logger.warn ("Could not write cartoon for row " + i + " glycan " + glycanId, e);
							}
        			        
        				}
        			} else {
        				cell.setCellStyle(wrapTextStyle);
        				cell.setCellValue(col);
        			}
        		}
        	}
        }
        
        //resize pictures
		for (Picture pic: m_lPictures) {
			pic.resize();
		}
        
        workbook.write(excelWriter);
        excelWriter.close();
        workbook.close();
	}
	
	public static BufferedImage scale(BufferedImage before, double scale) {
	    int w = before.getWidth();
	    int h = before.getHeight();
	    // Create a new image of the proper size
	    int w2 = (int) (w * scale);
	    int h2 = (int) (h * scale);
	    BufferedImage after = new BufferedImage(w2, h2, BufferedImage.TYPE_INT_ARGB);
	    AffineTransform scaleInstance = AffineTransform.getScaleInstance(scale, scale);
	    AffineTransformOp scaleOp 
	        = new AffineTransformOp(scaleInstance, AffineTransformOp.TYPE_BILINEAR);

	    scaleOp.filter(before, after);
	    return after;
	}
}
