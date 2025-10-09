package org.glygen.tablemaker.controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.glygen.tablemaker.persistence.dao.DatasetRepository;
import org.glygen.tablemaker.persistence.dao.DatasetSpecification;
import org.glygen.tablemaker.persistence.dao.GlycanImageRepository;
import org.glygen.tablemaker.persistence.dao.PublicationRepository;
import org.glygen.tablemaker.persistence.dao.TemplateRepository;
import org.glygen.tablemaker.persistence.dataset.Dataset;
import org.glygen.tablemaker.persistence.dataset.DatasetGlycoproteinMetadata;
import org.glygen.tablemaker.persistence.dataset.DatasetMetadata;
import org.glygen.tablemaker.persistence.dataset.DatasetProjection;
import org.glygen.tablemaker.persistence.dataset.Publication;
import org.glygen.tablemaker.persistence.protein.GlycoproteinColumns;
import org.glygen.tablemaker.persistence.table.GlycanColumns;
import org.glygen.tablemaker.persistence.table.TableColumn;
import org.glygen.tablemaker.persistence.table.TableMakerTemplate;
import org.glygen.tablemaker.view.DatasetTableDownloadView;
import org.glygen.tablemaker.view.DatasetView;
import org.glygen.tablemaker.view.Filter;
import org.glygen.tablemaker.view.GlygenMetadataRow;
import org.glygen.tablemaker.view.GlygenProteinMetadataRow;
import org.glygen.tablemaker.view.Sorting;
import org.glygen.tablemaker.view.SuccessResponse;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/public")
public class PublicDataController {
	static Logger logger = org.slf4j.LoggerFactory.getLogger(PublicDataController.class);
	
	final private DatasetRepository datasetRepository;
	final private GlycanImageRepository glycanImageRepository;
	final private TemplateRepository templateRepository;
	final private PublicationRepository publicationRepository;
	
	@Value("${spring.file.imagedirectory}")
    String imageLocation;
	
	@Value("${spring.file.uploaddirectory}")
	String uploadDir;
	
	public PublicDataController(DatasetRepository datasetRepository, GlycanImageRepository glycanImageRepository, TemplateRepository templateRepository, PublicationRepository publicationRepository) {
		this.datasetRepository = datasetRepository;
		this.glycanImageRepository = glycanImageRepository;
		this.templateRepository = templateRepository;
		this.publicationRepository = publicationRepository;
	}
	
	@Operation(summary = "Get public dataset's publications")
    @GetMapping("/getdatasetpublications")
    public ResponseEntity<SuccessResponse<Map<String, Object>>> getDatasetPublications(
    		@RequestParam(required=false, name="versionid")
            Long versionId,
            @RequestParam(required=false, name="datasetid")
            String datasetId,
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
		
		if (versionId == null && datasetId == null) {
			throw new IllegalArgumentException("One of versionid or datasetid should be provided");
		}
		// parse filters and sorting
        ObjectMapper mapper = new ObjectMapper();
        
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
        
        Page<Publication> publications = null;
        if (versionId != null) {
        	publications = publicationRepository.listPublications(versionId, globalFilter, PageRequest.of(start, size, Sort.by(sortOrders)));
        } else {
        	publications = publicationRepository.listPublications(datasetId, globalFilter, PageRequest.of(start, size, Sort.by(sortOrders)));
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("objects", publications.getContent());
        response.put("currentPage", publications.getNumber());
        response.put("totalItems", publications.getTotalElements());
        response.put("totalPages", publications.getTotalPages());
        
        return new ResponseEntity<>(new SuccessResponse<Map<String, Object>>(response, "publications retrieved"), HttpStatus.OK);
	}
	
	@Operation(summary = "Get public dataset's metadata")
    @GetMapping("/getdatasetdata")
    public ResponseEntity<SuccessResponse<Map<String, Object>>> getDatasetData(
    		@RequestParam(required=false, name="versionid")
            Long versionId,
            @RequestParam(required=false, name="datasetid")
            String datasetId,
            @RequestParam("type")
            String type,
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
		
		if (versionId == null && datasetId == null) {
			throw new IllegalArgumentException("One of versionid or datasetid should be provided");
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
        
        if (type.equalsIgnoreCase("glycoprotein")) { 	
        	Page<String> rows = null;
        	if (versionId != null) {
        		rows = datasetRepository.getGlycoproteinDataByVersion(versionId, globalFilter, filterList, PageRequest.of(start, size, Sort.by(sortOrders)));
            } else {
            	rows = datasetRepository.getGlycoproteinData(datasetId, globalFilter, filterList, PageRequest.of(start, size, Sort.by(sortOrders)));
            }
        	List<DatasetGlycoproteinMetadata> data = datasetRepository.findGlycoproteinByRowIdIn(rows.getContent());
        	
        	Map<String, List<DatasetGlycoproteinMetadata>> rowMap = new HashMap<>();
			for (DatasetGlycoproteinMetadata m: data) {
				if (rowMap.get(m.getRowId()) == null) {
					rowMap.put(m.getRowId(), new ArrayList<>());
				}
				rowMap.get(m.getRowId()).add(m);
			}
			
			List<GlygenProteinMetadataRow> result = new ArrayList<>();
			for (String key: rowMap.keySet()) {
				GlygenProteinMetadataRow row = new GlygenProteinMetadataRow();
				row.setRowId(key);
				row.setColumns(rowMap.get(key));
				for (DatasetGlycoproteinMetadata col: row.getColumns()) {
					if (col.getGlycoproteinColumn() != null && col.getGlycoproteinColumn() == GlycoproteinColumns.GLYTOUCANID) {
						try {
							byte[] cartoon = UtilityController.getCartoon(col.getValue().trim(), glycanImageRepository, imageLocation);
							row.setCartoon(cartoon);
						} catch (Exception e) {
							logger.warn("could not get the cartoon for " + col.getValue() + " column id: " + col.getId()) ;
						}
						break;
					}
				}
				result.add(row);
			}
        	
        	Map<String, Object> response = new HashMap<>();
            response.put("objects", result);
            response.put("currentPage", rows.getNumber());
            response.put("totalItems", rows.getTotalElements());
            response.put("totalPages", rows.getTotalPages());
            
            return new ResponseEntity<>(new SuccessResponse<Map<String, Object>>(response, "metadata retrieved"), HttpStatus.OK);
        } else {
        	Page<String> rows = null;
        	if (versionId != null) {
        		rows = datasetRepository.getDataByVersion(versionId, globalFilter, filterList, PageRequest.of(start, size, Sort.by(sortOrders)));
            } else {
            	rows = datasetRepository.getData(datasetId, globalFilter, filterList, PageRequest.of(start, size, Sort.by(sortOrders)));
            }
        	List<DatasetMetadata> data = datasetRepository.findByRowIdIn(rows.getContent());
        	Map<String, List<DatasetMetadata>> rowMap = new HashMap<>();
			for (DatasetMetadata m: data) {
				if (rowMap.get(m.getRowId()) == null) {
					rowMap.put(m.getRowId(), new ArrayList<>());
				}
				rowMap.get(m.getRowId()).add(m);
			}
			List<GlygenMetadataRow> result = new ArrayList<>();
			for (String key: rowMap.keySet()) {
				GlygenMetadataRow row = new GlygenMetadataRow();
				row.setRowId(key);
				row.setColumns(rowMap.get(key));
				for (DatasetMetadata col: row.getColumns()) {
					if (col.getGlycanColumn() != null && col.getGlycanColumn() == GlycanColumns.GLYTOUCANID) {
						try {
							byte[] cartoon = UtilityController.getCartoon(col.getValue().trim(), glycanImageRepository, imageLocation);
							row.setCartoon(cartoon);
						} catch (Exception e) {
							logger.warn("could not get the cartoon for " + col.getValue() + " column id: " + col.getId()) ;
						}
						break;
					}
				}
				result.add(row);
			}
			
        	Map<String, Object> response = new HashMap<>();
            response.put("objects", result);
            response.put("currentPage", rows.getNumber());
            response.put("totalItems", rows.getTotalElements());
            response.put("totalPages", rows.getTotalPages());
            
            return new ResponseEntity<>(new SuccessResponse<Map<String, Object>>(response, "metadata retrieved"), HttpStatus.OK);
        }
	}
	
	@Operation(summary = "Get public datasets")
    @GetMapping("/getdatasets")
    public ResponseEntity<SuccessResponse<Map<String, Object>>> getDatasets(
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
        	specificationList.add(new DatasetSpecification(new Filter ("dateCreated", globalFilter)));
        }
        
        Specification<Dataset> spec = null;
        if (!specificationList.isEmpty()) {
        	spec = specificationList.get(0);
        	for (int i=1; i < specificationList.size(); i++) {
        		spec = Specification.where(spec).or(specificationList.get(i)); 
        	}
        	if (globalFilter != null && !globalFilter.isBlank() && !globalFilter.equalsIgnoreCase("undefined")) {
        		spec = Specification.where(spec).or (DatasetSpecification.hasUserWithUsername(globalFilter));
        	}
        }
        
        Page<DatasetProjection> datasetProjectionsInPage = null;
        Page<Dataset> datasetsInPage = null;
        List<DatasetView> datasets = new ArrayList<>();
        Map<String, Object> response = new HashMap<>();
        if (spec != null) {
        	try {
        		datasetsInPage = datasetRepository.findAll(spec, PageRequest.of(start, size, Sort.by(sortOrders)));
        		for (Dataset d: datasetsInPage.getContent()) {
                	DatasetView dv = new DatasetView();
                	dv.setId(d.getDatasetId());
                	dv.setName(d.getName());
                	dv.setDatasetIdentifier(d.getDatasetIdentifier());
                	dv.setDateCreated(d.getDateCreated());
                	dv.setLicense(datasetRepository.getLicenseByDatasetId(d.getDatasetId()));
                	dv.setUser (d.getUser());
                	int proteinCount = datasetRepository.getProteinCount(d.getDatasetId());
                	dv.setNoProteins(proteinCount);
                	datasets.add(dv);
                }
        	} catch (Exception e) {
        		logger.error(e.getMessage(), e);
        		throw e;
        	}
        	
            response.put("objects", datasets);
            response.put("currentPage", datasetsInPage.getNumber());
            response.put("totalItems", datasetsInPage.getTotalElements());
            response.put("totalPages", datasetsInPage.getTotalPages());
        } else {
        	datasetProjectionsInPage = datasetRepository.findAllBy(PageRequest.of(start, size, Sort.by(sortOrders)));
        	for (DatasetProjection d: datasetProjectionsInPage.getContent()) {
            	DatasetView dv = new DatasetView();
            	dv.setId(d.getDatasetId());
            	dv.setName(d.getName());
            	dv.setDatasetIdentifier(d.getDatasetIdentifier());
            	dv.setDateCreated(d.getDateCreated());
            	dv.setLicense(datasetRepository.getLicenseByDatasetId(d.getDatasetId()));
            	dv.setUser (d.getUser());
            	int proteinCount = datasetRepository.getProteinCount(d.getDatasetId());
            	dv.setNoProteins(proteinCount);
            	datasets.add(dv);
            	response.put("objects", datasets);
                response.put("currentPage", datasetProjectionsInPage.getNumber());
                response.put("totalItems", datasetProjectionsInPage.getTotalElements());
                response.put("totalPages", datasetProjectionsInPage.getTotalPages());
            }
        }
         
        
        
        return new ResponseEntity<>(new SuccessResponse<Map<String, Object>>(response, "datasets retrieved"), HttpStatus.OK);
	}
	
	@Operation(summary = "Get dataset by the given id")
    @GetMapping("/getdataset/{datasetIdentifier}")
    public ResponseEntity<SuccessResponse<DatasetView>> getDataset(
    		@Parameter(required=true, description="id of the dataset to be retrieved") 
    		@PathVariable("datasetIdentifier") String datasetIdentifier) {
    	
        DatasetView dv = getDatasetFromRepository(datasetIdentifier);
        return new ResponseEntity<>(new SuccessResponse<DatasetView>(dv, "dataset retrieved"), HttpStatus.OK);
    }
	
	DatasetView getDatasetFromRepository (String datasetIdentifier) {
		String identifier = datasetIdentifier;
        String version = null;
        // check if the identifier contains a version
        String[] split = datasetIdentifier.split("-");
        if (split.length > 1) {
        	identifier = split[0];
        	version = split[1];
        }
        Dataset existing = null;
        if (version == null) {
        	existing = datasetRepository.findByDatasetIdentifierAndVersions_head(identifier, true);
        } else {
        	existing = datasetRepository.findByDatasetIdentifierAndVersions_version(identifier, version);
        }
        if (existing == null) {
            throw new IllegalArgumentException ("Could not find the given dataset " + datasetIdentifier);
        }
        
        DatasetView dv = DatasetController.createDatasetView (existing, version, glycanImageRepository, imageLocation);
        int proteinCount = 0;
        if (version != null) {
        	proteinCount = datasetRepository.getProteinCountByVersion(version);
        } else {
        	proteinCount = datasetRepository.getProteinCount(existing.getDatasetId());
        }
    	dv.setNoProteins(proteinCount);
        return dv;
	}
	
	@Operation(summary = "Generate table and download")
    @PostMapping("/downloadtable")
	public ResponseEntity<Resource> downloadTable (@Valid @RequestBody DatasetTableDownloadView table) {
		String filename = table.getFilename() != null ? table.getFilename() : "GlygenDataset";
		File newFile = new File (uploadDir + File.separator + filename + System.currentTimeMillis() + ".csv");
		
		try {
			if (table.getData() != null && !table.getData().isEmpty()) {
				// get GlygenTemplate
				TableMakerTemplate glygenTemplate = templateRepository.findById(1L).get();
				String[] header = new String[glygenTemplate.getColumns().size()];
				for (TableColumn col: glygenTemplate.getColumns()) {
					header[col.getOrder()-1] = col.getName();
				}
				List<String[]> rows = new ArrayList<>();
				// generate rows from the data
				rows.add(header);
				List<GlygenMetadataRow> data = table.getData();
				if (data != null) {
					for (GlygenMetadataRow r: data) {
						String[] row = new String[glygenTemplate.getColumns().size()];
						for (TableColumn col: glygenTemplate.getColumns()) {
							for (DatasetMetadata c: r.getColumns()) {
								if (col.getGlycanColumn() != null && col.getGlycanColumn().equals(c.getGlycanColumn())) {
									row[col.getOrder()-1] = c.getValue();
									break;
								} else if (col.getDatatype() != null && c.getDatatype() != null &&
										col.getDatatype().getName().equals (c.getDatatype().getName())) {
									switch (col.getType()) {
									case ID:
										String value = c.getValueId();
										if (c.getValueId()!= null && c.getValueId().startsWith("UBERON"))
											value = value.replace("_", ":");
										row[col.getOrder()-1] = value;
										break;
									case URI:
										row[col.getOrder()-1] = c.getValueUri();
										break;
									case VALUE:
										row[col.getOrder()-1] = c.getValue();
										break;
									default:
										row[col.getOrder()-1] = c.getValue();
										break;
									
									}
									break;
								}
							}
						}
						rows.add(row);
					}
				}
				TableController.writeToCSV(rows, newFile);
			} else if (table.getGlycoproteinData() != null && !table.getGlycoproteinData().isEmpty()) {
				// get GlygenTemplate
				TableMakerTemplate glygenTemplate = templateRepository.findById(2L).get();   // glycoprotein template
				String[] header = new String[glygenTemplate.getColumns().size()];
				for (TableColumn col: glygenTemplate.getColumns()) {
					header[col.getOrder()-1] = col.getName();
				}
				List<String[]> rows = new ArrayList<>();
				// generate rows from the data
				rows.add(header);
				List<GlygenProteinMetadataRow> data = table.getGlycoproteinData();
				if (data != null) {
					for (GlygenProteinMetadataRow r: data) {
						String[] row = new String[glygenTemplate.getColumns().size()];
						for (TableColumn col: glygenTemplate.getColumns()) {
							for (DatasetGlycoproteinMetadata c: r.getColumns()) {
								if (col.getProteinColumn() != null && col.getProteinColumn().equals(c.getGlycoproteinColumn())) {
									row[col.getOrder()-1] = c.getValue();
									break;
								} else if (col.getDatatype() != null && c.getDatatype() != null &&
										col.getDatatype().getName().equals (c.getDatatype().getName())) {
									switch (col.getType()) {
									case ID:
										String value = c.getValueId();
										if (c.getValueId()!= null && c.getValueId().startsWith("UBERON"))
											value = value.replace("_", ":");
										row[col.getOrder()-1] = value;
										break;
									case URI:
										row[col.getOrder()-1] = c.getValueUri();
										break;
									case VALUE:
										row[col.getOrder()-1] = c.getValue();
										break;
									default:
										row[col.getOrder()-1] = c.getValue();
										break;
									
									}
									break;
								}
							}
						}
						rows.add(row);
					}
				}
				TableController.writeToCSV(rows, newFile);
			} else {
				throw new IllegalArgumentException ("Failed to generate download file. There is no data!");
			}
			return FileController.download(newFile, filename+".csv", null);
		} catch (IOException e) {
			throw new IllegalArgumentException ("Failed to generate download file. Reason: " + e.getMessage());
		}
		
	}
	
	@Operation(summary = "Generate table for the given dataset and download")
    @GetMapping("/downloadtablefordataset/{datasetIdentifier}")
	@ApiResponses (value ={@ApiResponse(responseCode="200", description="File downloaded successfully"), 
            @ApiResponse(responseCode="415", description="Media type is not supported"),
            @ApiResponse(responseCode="500", description="Internal Server Error")})
	public ResponseEntity<Resource> downloadTableForDataset (
			@Parameter(required=true, description="id of the dataset to be retrieved") 
    		@PathVariable("datasetIdentifier") String datasetIdentifier,
    		@Parameter(required=false, description="filename to save the downloaded file as. If not provided, it will be automatically generated") 
    		@RequestParam(required=false, value="filename")
            String fileName) {
		
		DatasetView dv = getDatasetFromRepository(datasetIdentifier);
		
		DatasetTableDownloadView downloadView = new DatasetTableDownloadView();
		downloadView.setFilename(fileName);
		downloadView.setData(dv.getData());
		downloadView.setGlycoproteinData(dv.getGlycoproteinData());
		
		return downloadTable(downloadView);
	}
	
	
}
