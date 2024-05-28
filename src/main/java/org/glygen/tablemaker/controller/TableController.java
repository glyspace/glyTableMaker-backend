package org.glygen.tablemaker.controller;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.glygen.tablemaker.exception.BadRequestException;
import org.glygen.tablemaker.exception.DuplicateException;
import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.dao.TableReportRepository;
import org.glygen.tablemaker.persistence.dao.TemplateRepository;
import org.glygen.tablemaker.persistence.dao.UserRepository;
import org.glygen.tablemaker.persistence.glycan.Collection;
import org.glygen.tablemaker.persistence.glycan.GlycanInCollection;
import org.glygen.tablemaker.persistence.glycan.Metadata;
import org.glygen.tablemaker.persistence.table.FileFormat;
import org.glygen.tablemaker.persistence.table.GlycanColumns;
import org.glygen.tablemaker.persistence.table.TableColumn;
import org.glygen.tablemaker.persistence.table.TableMakerTemplate;
import org.glygen.tablemaker.persistence.table.TableReport;
import org.glygen.tablemaker.persistence.table.TableReportDetail;
import org.glygen.tablemaker.persistence.table.TableView;
import org.glygen.tablemaker.view.SuccessResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVWriter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/table")
public class TableController {
	
	@Value("${spring.file.uploaddirectory}")
	String uploadDir;
	
	final private UserRepository userRepository;
	final private TemplateRepository templateRepository;
	final private TableReportRepository reportRepository;
	
	public TableController(UserRepository userRepository, TemplateRepository templateRepository, TableReportRepository reportRepository) {
		this.userRepository = userRepository;
		this.templateRepository = templateRepository;
		this.reportRepository = reportRepository;
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
	
	@Operation(summary = "Generate report and download", security = { @SecurityRequirement(name = "bearer-key") })
    @PostMapping("/downloadtable")
	public ResponseEntity<Resource> downloadTable (@Valid @RequestBody TableView table) {
		String filename = "tablemakerexport";
		String ext = (table.getFileFormat() == FileFormat.EXCEL ? ".xlsx" : ".csv");
		File newFile = new File (uploadDir + File.pathSeparator + filename + System.currentTimeMillis() + ext);
		
		List<Collection> collectionList = new ArrayList<>();
		for (Collection c: table.getCollections()) {
			if (c.getCollections() != null) {
				collectionList.addAll(c.getCollections());
			} else {
				collectionList.add(c);
			}
		}
		List<String[]> rows = new ArrayList<>();
		Map<String, byte[]> cartoons = new HashMap<>();
		
		TableReport tableReport = new TableReport();
		TableReportDetail report = new TableReportDetail();
		
		if (table.getFileFormat() == FileFormat.CSV) {
			for (TableColumn col: table.getColums()) {
				if (col.getGlycanColumn() != null) {
					if (col.getGlycanColumn() == GlycanColumns.CARTOON) {
						throw new BadRequestException("Cartoons cannot be written in CSV files");
					}
				}
			}
		}
		
		for (Collection c: collectionList) {
			for (GlycanInCollection g: c.getGlycans()) {
				String[] row = new String[table.getColums().size()];
				rows.add(row);
				int i=0;
				for (TableColumn col: table.getColums()) {
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
									report.addWarning("Glycan " + g.getGlycan().getGlycanId() + " in collection " + c.getName() + " does not have a cartoon. Column left empty");
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
						boolean found = false;
						for (Metadata metadata: c.getMetadata()) {
							if (metadata.getType().getDatatypeId().equals(col.getDatatype().getDatatypeId())) {
								switch (col.getType()) {
								case ID:
									if (!metadata.getType().getNamespace().getHasId()) {
										// ERROR!
										report.addError(metadata.getType().getNamespace().getName() + " does not have value ID but \"ID\" is requested for " + col.getName() + " column");
									} else {
										if (metadata.getValueId() == null && col.getDefaultValue() != null) {
											row[i] = col.getDefaultValue();
										} else {
											row[i] = metadata.getValueId();
										}
									}
									break;
								case URI:
									if (!metadata.getType().getNamespace().getHasUri()) {
										// ERROR!
										report.addError(metadata.getType().getNamespace().getName() + " does not have value URI but \"URI\" is requested for " + col.getName() + " column");
									} else {
										if (metadata.getValueUri() == null && col.getDefaultValue() != null) {
											row[i] = col.getDefaultValue();
										} else {
											row[i] = metadata.getValueUri();
										}
									}
									break;
								case VALUE:
								default:
									if (metadata.getValue() == null && col.getDefaultValue() != null) {
										row[i] = col.getDefaultValue();
									} else {
										row[i] = metadata.getValue();
									}
									break;
								}
							}
							break;
						}
						if (!found) {
							// add a warning to the report
							// warning
							report.addWarning("Collection " + c.getName() + " does not have metadata for column " + col.getName() + ". Column is left empty!");
							row[i] = "";
							
						}
					} else if (col.getDefaultValue() != null) {
						row[i] = col.getDefaultValue();
					} else {
						row[i] = "";
					}
					i++;
				}
			}
		}
		try {
			// create the file
			if (table.getFileFormat() == FileFormat.EXCEL) {
				writeToExcel (rows, cartoons, newFile);
			} else {
				writeToCSV(rows, newFile);
			}
		} catch (IOException e) {
			throw new RuntimeException ("Failed to generate the report", e);
		}
		// save the report
		try {
			String reportJson = new ObjectMapper().writeValueAsString(report);
			tableReport.setReportJSON(reportJson);
			TableReport saved = reportRepository.save(tableReport);
			return FileController.download(newFile, filename+ext, saved.getReportId()+"");
		} catch (JsonProcessingException e) {
			throw new RuntimeException ("Failed to generate the report", e);
		}	
	}

	private void writeToCSV(List<String[]> rows, File newFile) throws IOException {
		try (CSVWriter writer = new CSVWriter(new FileWriter(newFile.getPath().toString()))) {
	        writer.writeAll(rows);
	    }
	}

	private void writeToExcel(List<String[]> rows, Map<String, byte[]> cartoons, File newFile) {
		// TODO Auto-generated method stub
		
	}
}
