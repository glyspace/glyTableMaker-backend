package org.glygen.tablemaker.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.glygen.tablemaker.controller.DataController;
import org.glygen.tablemaker.exception.DataNotFoundException;
import org.glygen.tablemaker.exception.GlytoucanAPIFailedException;
import org.glygen.tablemaker.exception.GlytoucanFailedException;
import org.glygen.tablemaker.persistence.ApplicationSettingsEntity;
import org.glygen.tablemaker.persistence.BatchUploadJob;
import org.glygen.tablemaker.persistence.ErrorReportEntity;
import org.glygen.tablemaker.persistence.GlycanImageEntity;
import org.glygen.tablemaker.persistence.dao.ApplicationSettingsRepository;
import org.glygen.tablemaker.persistence.dao.BatchUploadJobRepository;
import org.glygen.tablemaker.persistence.dao.BatchUploadRepository;
import org.glygen.tablemaker.persistence.dao.DatasetRepository;
import org.glygen.tablemaker.persistence.dao.GlycanImageRepository;
import org.glygen.tablemaker.persistence.dao.GlycanRepository;
import org.glygen.tablemaker.persistence.dao.UserRepository;
import org.glygen.tablemaker.persistence.dataset.DatabaseResource;
import org.glygen.tablemaker.persistence.dataset.DatabaseResourceDataset;
import org.glygen.tablemaker.persistence.dataset.Dataset;
import org.glygen.tablemaker.persistence.glycan.Glycan;
import org.glygen.tablemaker.persistence.glycan.RegistrationStatus;
import org.glygen.tablemaker.persistence.glycan.UploadStatus;
import org.glygen.tablemaker.util.GlytoucanUtil;
import org.glygen.tablemaker.util.SequenceUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;

@Transactional
@Service
public class ScheduledTasksService {
	
	final static Logger logger = LoggerFactory.getLogger(ScheduledTasksService.class);
	
	final private BatchUploadJobRepository batchUploadJobRepository;
	final private AsyncService batchUploadService;
	final private BatchUploadRepository uploadRepository;
	final private GlycanRepository glycanRepository;
	final private ErrorReportingService errorReportingService;
	final private UserRepository userRepository;
	final private GlycanImageRepository glycanImageRepository;
	final private DatasetRepository datasetRepository;
	final private ApplicationSettingsRepository settingRepository;
	final private DatasetManager datasetManager;
	final private EmailManager emailManager;
	
	@Value("${spring.file.uploaddirectory}")
	String uploadDir;
	
	@Value("${spring.file.imagedirectory}")
    String imageLocation;
	
	@Value("${glygen.releaseURL}")
	String glygenURL;
	
	@Value("${glygen.versionURL}")
	String versionURL;
			
	
	public ScheduledTasksService(AsyncService batchUploadService, BatchUploadJobRepository batchUploadJobRepository, GlycanRepository glycanRepository, BatchUploadRepository uploadRepository, ErrorReportingService errorReportingService, UserRepository userRepository, GlycanImageRepository glycanImageRepository, DatasetRepository datasetRepository, ApplicationSettingsRepository settingRepository, DatasetManager datasetManager, EmailManager emailManager) {
		this.batchUploadJobRepository = batchUploadJobRepository;
		this.batchUploadService = batchUploadService;
		this.uploadRepository = uploadRepository;
		this.glycanRepository = glycanRepository;
		this.errorReportingService = errorReportingService;
		this.userRepository = userRepository;
		this.glycanImageRepository = glycanImageRepository;
		this.datasetRepository = datasetRepository;
		this.settingRepository = settingRepository;
		this.datasetManager = datasetManager;
		this.emailManager = emailManager;
	}
	
	@Scheduled(fixedDelay = 604800000, initialDelay=2000)
    public void checkGlyGenIntegration () {
		String lastCheckedGlygenVersion = null;
		ApplicationSettingsEntity entity = null;
		
		try {
			// check Glygen version, last integrated 
			Optional<ApplicationSettingsEntity> s = settingRepository.findByName("GlyGen");
			if (s.isPresent()) {
				entity = s.get();
				lastCheckedGlygenVersion = entity.getValue();
			} else {
				entity = new ApplicationSettingsEntity();
				entity.setName("GlyGen");
			}
			
			//retrieve current Glygen version using Glygen API
			String currentGlygenVersion = getGlygenVersion();
			
			if (lastCheckedGlygenVersion == null || !lastCheckedGlygenVersion.equalsIgnoreCase(currentGlygenVersion)) {
				// we need to get the new list and update existing integrated datasets
				List<String> datasetUrls = getDatasetUrlsFromGlygen();
				List<String> identifiers = new ArrayList<>();
				
				Map<String, String> mappings = null;
				for (String datasetURL: datasetUrls) {
					String id= datasetURL.substring(datasetURL.lastIndexOf("/")+1, datasetURL.indexOf(".json"));
					// if the identifier is using the old scheme ("TD") - find the corresponding new identifier
					if (id.startsWith("TD") && mappings == null) {
						mappings = readMappings();
					}
					if (id.startsWith("TD") && mappings != null) {
						id = mappings.get(id);
					}
					if (id != null) {
						identifiers.add(id);
					} else {
						// error, dataset removed in tablemaker ??
						logger.error("Given dataset identifier " + id + " does not exist in the repository");
					}
				}
				
				// check already (GlyGen) integrated datasets in Tablemaker
				List<Dataset> integratedDatasets = datasetRepository.getDatasetsIntegratedIn("GlyGen");
				for (Dataset d: integratedDatasets) {
					if (!identifiers.contains(d.getDatasetIdentifier())) {
						List<DatabaseResourceDataset> updatedResources = new ArrayList<>();
						// removed from GlyGen
						for (DatabaseResourceDataset resource: d.getIntegratedIn()) {
							if (resource.getResource() != null && !resource.getResource().getName().equalsIgnoreCase("GlyGen")) {
								updatedResources.add(resource);
							}
						}
						
						//notify the user of the dataset; send an email to d.getUser()
						try {
							emailManager.sendDatasetRemovedFromGlyGenNotice(d.getUser(), d.getDatasetIdentifier(), currentGlygenVersion);
						} catch (Exception e) {
							logger.warn ("GlyGen integration removed notification could not be sent to user: " + d.getUser().getUsername(), e);
						}
						d.getIntegratedIn().clear();
						d.getIntegratedIn().addAll(updatedResources);
						datasetManager.saveDataset(d);
					}
				}
				
				for (String datasetURL: datasetUrls) {
					String id = datasetURL.substring(datasetURL.lastIndexOf("/")+1, datasetURL.indexOf(".json"));
					// if the identifier is using the old scheme ("TD") - find the corresponding new identifier
					if (id.startsWith("TD") && mappings != null) {
						id = mappings.get(id);
					}
					if (id == null) {
						logger.error("Given dataset identifier " + id + " does not exist in the repository");
						continue;
					}
					Dataset dataset = datasetRepository.findByDatasetIdentifierAndVersions_head(id, true);
					if (dataset != null) {
						DatabaseResourceDataset resource = createGlygenResource(datasetURL, dataset);
						resource.setVersionInResource(currentGlygenVersion);
						if (dataset.getIntegratedIn() == null) {
							dataset.setIntegratedIn(new ArrayList<>());
						}
						dataset.getIntegratedIn().add(resource);
						
						datasetManager.saveDataset(dataset);
						
						// TODO send email to the user with excluded records. Do we save the info into the database?
					} else {
						logger.error("Given dataset identifier " + id + " does not exist in the repository");
					}
				}
				
				// update last checked version
				entity.setValue(currentGlygenVersion);
				settingRepository.save(entity);
				
			}
		} catch (Exception e) {
			logger.error("error checking glygen integrated datasets", e);
		}
	}
	
	Map<String, String> readMappings () {
		ObjectMapper objectMapper = new ObjectMapper();
		Map<String, String> mappings;
		try {
            ClassPathResource resource = new ClassPathResource("datasetidentifiermap.json");
            try (InputStream is = resource.getInputStream()) {
                mappings = objectMapper.readValue(
                        is,
                        new TypeReference<Map<String, String>>() {}
                );
            }
        } catch (Exception e) {
            // log and default safe
            throw new IllegalStateException("Failed to load datasetidentifiermap.json from classpath", e);
        }
		
		return mappings;

	}
	
	private String getGlygenVersion() throws IOException {
		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
	        HttpGet request = new HttpGet(versionURL);
	        HttpResponse response = httpClient.execute(request);
	        if (response.getStatusLine().getStatusCode() > 300) {
	        	throw new IOException ("Error getting dataset json from GlyGen: " + response.getStatusLine().getReasonPhrase());
	        }
	        String json = EntityUtils.toString(response.getEntity());

	        List<String> versions = parseJsonArrayOfStrings(json);

	        String highest = versions.stream().max(ScheduledTasksService::compareSemver).orElse(null);
	        return highest;
		}
    }
	
    private static int compareSemver(String a, String b) {
        int[] va = parseVersion(a);
        int[] vb = parseVersion(b);
        for (int i = 0; i < 3; i++) {
            if (va[i] != vb[i]) return Integer.compare(va[i], vb[i]);
        }
        return 0; 
    }

    private static int[] parseVersion(String v) {
        String[] parts = v.split("\\.");
        int[] out = new int[]{0, 0, 0};
        for (int i = 0; i < Math.min(3, parts.length); i++) {
            out[i] = Integer.parseInt(parts[i]);
        }
        return out;
    }

    private static List<String> parseJsonArrayOfStrings(String json) {
        // Remove leading/trailing brackets
        String inner = json.trim();
        if (inner.startsWith("[")) inner = inner.substring(1);
        if (inner.endsWith("]")) inner = inner.substring(0, inner.length() - 1);
        
        List<String> list = new ArrayList<>();
        String[] items = inner.split("\\s*,\\s*");
        for (String item : items) {
            String cleaned = item.trim();
            // remove one leading and trailing quote
            if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length() >= 2) {
                cleaned = cleaned.substring(1, cleaned.length() - 1);
            }
            list.add(cleaned);
        }
        return list;
    }


	private DatabaseResourceDataset createGlygenResource(String datasetURL, Dataset dataset) throws ParseException, IOException {
		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
	        HttpGet request = new HttpGet(datasetURL);
	        HttpResponse response = httpClient.execute(request);
	        if (response.getStatusLine().getStatusCode() > 300) {
	        	throw new IOException ("Error getting dataset json from GlyGen: " + response.getStatusLine().getReasonPhrase());
	        }
	        String json = EntityUtils.toString(response.getEntity());
	        DatabaseResource resource = new DatabaseResource();
	        resource.setName("GlyGen");
	        DatabaseResourceDataset dd = new DatabaseResourceDataset();
			dd.setDataset(dataset);
			dd.setResource(resource);
			dd.setDate(new Date());
			
	        JSONObject obj = new JSONObject(json);
	        JSONArray usage = obj.getJSONArray("usage");
	        if (usage.length() > 0) {
//	        if (!usage.isEmpty()) {
	        	JSONObject u = usage.getJSONObject(0);
	        	resource.setIdentifier(u.getString("bco_id"));
	        	resource.setURL(u.getString("dataset_url"));
	        	
	        	dd.setVersion("head");   //TODO get the version from the json file
	        }
	        return dd;
		}   
	}

	List<String> getDatasetUrlsFromGlygen () throws ParseException, IOException {
		List<String> identifiers = new ArrayList<>();
		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
	        HttpGet request = new HttpGet(glygenURL);
	        HttpResponse response = httpClient.execute(request);
	        if (response.getStatusLine().getStatusCode() > 300) {
	        	throw new IOException ("Error getting dataset list from GlyGen: " + response.getStatusLine().getReasonPhrase());
	        }
	        String json = EntityUtils.toString(response.getEntity());
	        
	        JSONArray datasets = new JSONArray(json);
			for (int i=0; i < datasets.length(); i++) {
				identifiers.add(datasets.getString(i));
			}
        }
		
		return identifiers;
	}
	
    @Scheduled(fixedDelay = 86400000, initialDelay=1000)
    public void checkGlytoucanRegistration () {
    	logger.info("Checking submitted glycans on " + new Date());
    	// check for Glycans with glytoucan hash or "not submitted yet" status
    	List<Glycan> submitted = glycanRepository.findByStatus(RegistrationStatus.NEWLY_SUBMITTED_FOR_REGISTRATION);
    	// try to get accession numbers for newly submitted ones
    	for (Glycan glycan: submitted) {
    		boolean modified = false;
    		if (glycan.getGlytoucanID() == null || glycan.getGlytoucanID().isEmpty()) {
    			try {
    				String glytoucanId = GlytoucanUtil.getInstance().getAccessionNumber(glycan.getWurcs());
    				if (glytoucanId != null) {
    					glycan.setGlytoucanID(glytoucanId);
    					glycan.setStatus(RegistrationStatus.NEWLY_REGISTERED);
    					modified = true;
    				}	
    			} catch (GlytoucanAPIFailedException e) {
    				logger.error (e.getMessage(), e);
    				// report the issue
    				ErrorReportEntity error = new ErrorReportEntity();
    				error.setMessage(e.getMessage());
    				error.setDetails("Error occurred during retrieval of glytoucan ids for the newly registered glycans tasks");
    				error.setDateReported(new Date());
    				error.setTicketLabel("GlytoucanAPI");
    				errorReportingService.reportError(error);
    				// try to check another way
    				try {
    					String glytoucanId = GlytoucanUtil.getInstance().checkBatchStatus(glycan.getGlytoucanHash());
        				if (glytoucanId != null) {
        					glycan.setGlytoucanID (glytoucanId);
        					glycan.setStatus(RegistrationStatus.NEWLY_REGISTERED);
        					modified = true;
        				}
        			} catch (GlytoucanFailedException e1) {
        				// there is an error
        				glycan.setError("Error registering. No additional information received from GlyTouCan.");
                		glycan.setStatus(RegistrationStatus.ERROR);
                		glycan.setErrorJson(e1.getErrorJson());
                		modified = true;
        			}
    				
    			}
    		} else {
    			// fix the status
    			glycan.setStatus(RegistrationStatus.NEWLY_REGISTERED);
				modified = true;
    		}
    		if (glycan.getGlytoucanID() == null || glycan.getGlytoucanID().isEmpty()) {
    			// if still empty, need to check the status
    			try {
    				String glytoucanId = GlytoucanUtil.getInstance().checkBatchStatus(glycan.getGlytoucanHash());
    				if (glytoucanId != null) {
    					glycan.setGlytoucanID (glytoucanId);
    					glycan.setStatus(RegistrationStatus.NEWLY_REGISTERED);
    					modified = true;
    				}
    			} catch (GlytoucanFailedException e) {
    				// there is an error
    				glycan.setError("Error registering. No additional information received from GlyTouCan.");
            		glycan.setStatus(RegistrationStatus.ERROR);
            		glycan.setErrorJson(e.getErrorJson());
            		modified = true;
    			}
    		}
    		if (modified) glycanRepository.save(glycan);
    		
    		// save cartoons in imagerepository as necessary
    		Optional<GlycanImageEntity> imageHandle = glycanImageRepository.findByGlycanId(glycan.getGlycanId());
        	if (!imageHandle.isPresent()) {
        		// create entry
        		GlycanImageEntity entity = new GlycanImageEntity();
        		entity.setGlycanId(glycan.getGlycanId());
        		entity.setGlytoucanId(glycan.getGlytoucanID());
        		entity.setWurcs(glycan.getWurcs());
        		glycanImageRepository.save(entity);
        	}
            try {
            	DataController.getImageForGlycan(imageLocation, glycan);
                if (glycan.getGlytoucanID() != null) {
            		List<GlycanImageEntity> images = glycanImageRepository.findByGlytoucanId(glycan.getGlytoucanID());
        			if (images == null || images.isEmpty()) {
        				// update glycan image table
            			imageHandle = glycanImageRepository.findByGlycanId(glycan.getGlycanId());
            			if (imageHandle.isPresent()) {
            				GlycanImageEntity entity = imageHandle.get();
            				entity.setGlytoucanId(glycan.getGlytoucanID());
            				glycanImageRepository.save(entity);
            			}
            			else {
            				logger.error("Cannot find glycan with id " + glycan.getGlycanId() + " in the image repository");
            			}
        			}
            	}
            } catch (DataNotFoundException e) {
                // ignore
                logger.warn ("no image found for glycan " + glycan.getGlycanId());
            }
    	}
    	List<Glycan> notYetSubmitted = glycanRepository.findByStatus(RegistrationStatus.NOT_SUBMITTED_YET);
    	for (Glycan glycan: notYetSubmitted) {
    		// check and register
    		try {
    			String glyToucanId = GlytoucanUtil.getInstance().getAccessionNumber(glycan.getWurcs());
    	        if (glyToucanId != null) {
    	            glycan.setGlytoucanID(glyToucanId);
    	            glycan.setStatus(RegistrationStatus.ALREADY_IN_GLYTOUCAN);
    	        } 
    	        // cannot get accession number -> register
    	        if (glycan.getGlytoucanID() == null || glycan.getGlytoucanID().isEmpty()) {
                	SequenceUtils.registerGlycan(glycan);
                }
    	        glycanRepository.save(glycan);
    	        
    	        // save cartoons in imagerepository as necessary
        		Optional<GlycanImageEntity> imageHandle = glycanImageRepository.findByGlycanId(glycan.getGlycanId());
            	if (!imageHandle.isPresent()) {
            		// create entry
            		GlycanImageEntity entity = new GlycanImageEntity();
            		entity.setGlycanId(glycan.getGlycanId());
            		entity.setGlytoucanId(glycan.getGlytoucanID());
            		entity.setWurcs(glycan.getWurcs());
            		glycanImageRepository.save(entity);
            	}
                try {
                	glycan.setCartoon(DataController.getImageForGlycan(imageLocation, glycan.getGlycanId()));
                    if (glycan.getGlytoucanID() != null) {
                		List<GlycanImageEntity> images = glycanImageRepository.findByGlytoucanId(glycan.getGlytoucanID());
            			if (images == null || images.isEmpty()) {
            				// update glycan image table
                			imageHandle = glycanImageRepository.findByGlycanId(glycan.getGlycanId());
                			if (imageHandle.isPresent()) {
                				GlycanImageEntity entity = imageHandle.get();
                				entity.setGlytoucanId(glycan.getGlytoucanID());
                				glycanImageRepository.save(entity);
                			}
                			else {
                				logger.error("Cannot find glycan with id " + glycan.getGlycanId() + " in the image repository");
                			}
            			}
                	}
                } catch (DataNotFoundException e) {
                    // ignore
                    logger.warn ("no image found for glycan " + glycan.getGlycanId());
                }
    		} catch (GlytoucanAPIFailedException e) {
    			// report the issue
				logger.error (e.getMessage(), e);
				ErrorReportEntity error = new ErrorReportEntity();
				error.setMessage(e.getMessage());
				error.setDetails("Error occurred during retrieval of glytoucan ids for \"not submitted\" glycans tasks");
				error.setDateReported(new Date());
				error.setTicketLabel("GlytoucanAPI");
				errorReportingService.reportError(error);
				break;
    		}
    	}
    	
    	logger.info("DONE Checking glycans: " + new Date());
    	
    	logger.info("Checking upload (waiting) jobs: " + new Date());
    	
    	//check the batch upload jobs waiting on glytoucan registration and resume them
    	List<BatchUploadJob> jobs = batchUploadJobRepository.findAll();
    	for (BatchUploadJob job: jobs) {
    		if (job.getUpload() != null && job.getUpload().getStatus() == UploadStatus.WAITING) {
    			logger.info("Restarting job: " + job.getJobId());
    			// restart the job
    			File uploadFolder = new File (uploadDir + File.separator + job.getUpload().getId());
    			File newFile = new File (uploadFolder + File.separator + job.getUpload().getFilename());
    			try {
	    			DataController.rerunAddGlycoproteinsFromFile(this, newFile, job.getUpload().getId(), job.getFileType(), 
	    					job.getTag(), job.getOrderParam(), job.getCompType(), job.getUser().getUserId(), 
	    					uploadRepository, batchUploadJobRepository, batchUploadService, userRepository);
	    			job.setLastRun(new Date());
	    			batchUploadJobRepository.save(job);
    			} catch (Exception e) {
    				logger.error("Upload job resumed but failed: " + e.getMessage());
    			}
    		}
    	}
    	logger.info("DONE Checking upload jobs: " + new Date());
    }
}
