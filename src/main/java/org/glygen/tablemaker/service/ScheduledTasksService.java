package org.glygen.tablemaker.service;

import java.io.File;
import java.util.Date;
import java.util.List;

import org.glygen.tablemaker.controller.DataController;
import org.glygen.tablemaker.exception.GlytoucanAPIFailedException;
import org.glygen.tablemaker.exception.GlytoucanFailedException;
import org.glygen.tablemaker.persistence.BatchUploadJob;
import org.glygen.tablemaker.persistence.ErrorReportEntity;
import org.glygen.tablemaker.persistence.dao.BatchUploadJobRepository;
import org.glygen.tablemaker.persistence.dao.BatchUploadRepository;
import org.glygen.tablemaker.persistence.dao.GlycanRepository;
import org.glygen.tablemaker.persistence.dao.UserRepository;
import org.glygen.tablemaker.persistence.glycan.Glycan;
import org.glygen.tablemaker.persistence.glycan.RegistrationStatus;
import org.glygen.tablemaker.persistence.glycan.UploadStatus;
import org.glygen.tablemaker.util.GlytoucanUtil;
import org.glygen.tablemaker.util.SequenceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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
	
	@Value("${spring.file.uploaddirectory}")
	String uploadDir;
	
	public ScheduledTasksService(AsyncService batchUploadService, BatchUploadJobRepository batchUploadJobRepository, GlycanRepository glycanRepository, BatchUploadRepository uploadRepository, ErrorReportingService errorReportingService, UserRepository userRepository) {
		this.batchUploadJobRepository = batchUploadJobRepository;
		this.batchUploadService = batchUploadService;
		this.uploadRepository = uploadRepository;
		this.glycanRepository = glycanRepository;
		this.errorReportingService = errorReportingService;
		this.userRepository = userRepository;
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
