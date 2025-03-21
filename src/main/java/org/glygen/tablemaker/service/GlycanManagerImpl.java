package org.glygen.tablemaker.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.glygen.tablemaker.exception.GlytoucanAPIFailedException;
import org.glygen.tablemaker.exception.GlytoucanFailedException;
import org.glygen.tablemaker.persistence.BatchUploadEntity;
import org.glygen.tablemaker.persistence.UploadErrorEntity;
import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.dao.BatchUploadRepository;
import org.glygen.tablemaker.persistence.dao.GlycanInFileRepository;
import org.glygen.tablemaker.persistence.dao.GlycanRepository;
import org.glygen.tablemaker.persistence.dao.GlycanTagRepository;
import org.glygen.tablemaker.persistence.dao.GlycoproteinInFileRepository;
import org.glygen.tablemaker.persistence.dao.GlycoproteinRepository;
import org.glygen.tablemaker.persistence.dao.SiteRepository;
import org.glygen.tablemaker.persistence.dao.UploadErrorRepository;
import org.glygen.tablemaker.persistence.glycan.Glycan;
import org.glygen.tablemaker.persistence.glycan.GlycanInFile;
import org.glygen.tablemaker.persistence.glycan.GlycanTag;
import org.glygen.tablemaker.persistence.glycan.RegistrationStatus;
import org.glygen.tablemaker.persistence.protein.Glycoprotein;
import org.glygen.tablemaker.persistence.protein.GlycoproteinInFile;
import org.glygen.tablemaker.persistence.protein.Site;
import org.glygen.tablemaker.util.GlytoucanUtil;
import org.glygen.tablemaker.util.SequenceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Transactional
@Service
public class GlycanManagerImpl implements GlycanManager {
	
	final static Logger logger = LoggerFactory.getLogger(GlycanManager.class);

	final private GlycanRepository glycanRepository;
	final private GlycanTagRepository glycanTagRepository;
	final private BatchUploadRepository uploadRepository;
	final private GlycanInFileRepository glycanInFileRepository;
	final private GlycoproteinInFileRepository glycoproteinInFileRepository;
	final private UploadErrorRepository uploadErrorRepository;
	final private GlycoproteinRepository glycoproteinRepository;
	final private SiteRepository siteRepository;
    
    public GlycanManagerImpl(GlycanTagRepository glycanTagRepository, GlycanRepository glycanRepository, BatchUploadRepository uploadRepository, GlycanInFileRepository glycanInFileRepository, UploadErrorRepository uploadErrorRepository, GlycoproteinInFileRepository glycoproteinInFileRepository, GlycoproteinRepository glycoproteinRepository, SiteRepository siteRepository) {
		this.glycanRepository = glycanRepository;
		this.glycanTagRepository = glycanTagRepository;
		this.uploadRepository = uploadRepository;
		this.glycanInFileRepository = glycanInFileRepository;
		this.glycoproteinInFileRepository = glycoproteinInFileRepository;
		this.uploadErrorRepository = uploadErrorRepository;
		this.glycoproteinRepository = glycoproteinRepository;
		this.siteRepository = siteRepository;
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
    				//TODO report the issue
    				logger.error (e.getMessage(), e);
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
    			//TODO report the issue
				logger.error (e.getMessage(), e);
				break;
    		}
    	}
    }
    
	@Override
	public void addTagToGlycans(Collection<Glycan> glycans, String tag, UserEntity user) {
		if (glycans != null) {
			GlycanTag gTag = new GlycanTag();
    		gTag.setLabel(tag);
    		gTag.setUser(user);
    		GlycanTag existing = glycanTagRepository.findByUserAndLabel(user, tag);
    		if (existing == null) {
    			existing = glycanTagRepository.save(gTag);
    		}
        	for (Glycan g: glycans) {
        		if (!g.hasTag(existing.getLabel())) {
        			g.addTag (existing);
        			glycanRepository.save(g);
        		}
        	}
        }
	}
	
	@Override
	public Glycan addUploadToGlycan (Glycan glycan, BatchUploadEntity upload, Boolean isNew, UserEntity user) {
		if (glycan != null) {
			GlycanInFile u = new GlycanInFile();
            u.setUploadFile(upload);
            if (upload.getGlycans() == null) {
            	upload.setGlycans(new ArrayList<>());
            }
            if (upload.getGlycoproteins() == null) {
            	upload.setGlycoproteins(new ArrayList<>());
            }
            if (upload.getErrors() == null) {
            	upload.setErrors(new ArrayList<>());
            }
            u.setGlycan(glycan);
            u.setIsNew(isNew);
            if (glycan.getUploadFiles() == null) 
            	glycan.setUploadFiles(new ArrayList<>());
            glycan.getUploadFiles().add(u);
            Glycan added = glycanRepository.save(glycan);
            upload.getGlycans().add(u);
            uploadRepository.save(upload);
            return added;
        }
		return null;
	}
	
	@Override
	public List<GlycanTag> getTags(UserEntity user) {
		return new ArrayList<>(glycanTagRepository.findAllByUser(user));
	}

	@Override
	public void setGlycanTags(Glycan glycan, List<String> tags, UserEntity user) {
		List<GlycanTag> newTagList = new ArrayList<>();
		Set<String> noDuplicates = new HashSet<>(tags);
		for (String tag: noDuplicates) {
			GlycanTag gTag = new GlycanTag();
			gTag.setLabel(tag);
			gTag.setUser(user);
			GlycanTag existing = glycanTagRepository.findByUserAndLabel(user, tag);
			if (existing == null) {
				existing = glycanTagRepository.save(gTag);
			}
			newTagList.add(existing);
		}
		glycan.setTags(newTagList);
		glycanRepository.save(glycan);
		
	}
	
	@Override
	public void setGlycoproteinTags(Glycoprotein glycoprotein, List<String> tags, UserEntity user) {
		List<GlycanTag> newTagList = new ArrayList<>();
		Set<String> noDuplicates = new HashSet<>(tags);
		for (String tag: noDuplicates) {
			GlycanTag gTag = new GlycanTag();
			gTag.setLabel(tag);
			gTag.setUser(user);
			GlycanTag existing = glycanTagRepository.findByUserAndLabel(user, tag);
			if (existing == null) {
				existing = glycanTagRepository.save(gTag);
			}
			newTagList.add(existing);
		}
		glycoprotein.setTags(newTagList);
		glycoproteinRepository.save(glycoprotein);
		
	}
	
	@Override
	public void deleteBatchUpload (BatchUploadEntity upload) {
		for (GlycanInFile g: upload.getGlycans()) {
			glycanInFileRepository.delete(g);
			g.setUploadFile(null);
		}
		for (GlycoproteinInFile g: upload.getGlycoproteins()) {
			glycoproteinInFileRepository.delete(g);
			g.setUploadFile(null);
		}
		for (UploadErrorEntity e: upload.getErrors()) {
			uploadErrorRepository.delete(e);
			e.setUpload(null);
		}
		uploadRepository.delete(upload);
	}
	
	@Override
	public Glycoprotein saveGlycoProtein (Glycoprotein p) {
		Glycoprotein saved = glycoproteinRepository.save(p);
		for (Site s: p.getSites()) {
			Site savedS = siteRepository.save(s);
			if (savedS.getGlycans() == null)
				System.out.println ("Error!!!");
		}
		return saved;
	}
}
