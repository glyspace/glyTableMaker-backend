package org.glygen.tablemaker.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.glygen.tablemaker.persistence.protein.Glycoprotein;
import org.glygen.tablemaker.persistence.protein.GlycoproteinInFile;
import org.glygen.tablemaker.persistence.protein.Site;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	
    public GlycanManagerImpl(GlycanTagRepository glycanTagRepository, GlycanRepository glycanRepository, BatchUploadRepository uploadRepository, 
    		GlycanInFileRepository glycanInFileRepository, UploadErrorRepository uploadErrorRepository, 
    		GlycoproteinInFileRepository glycoproteinInFileRepository, GlycoproteinRepository glycoproteinRepository, SiteRepository siteRepository) {
		this.glycanRepository = glycanRepository;
		this.glycanTagRepository = glycanTagRepository;
		this.uploadRepository = uploadRepository;
		this.glycanInFileRepository = glycanInFileRepository;
		this.glycoproteinInFileRepository = glycoproteinInFileRepository;
		this.uploadErrorRepository = uploadErrorRepository;
		this.glycoproteinRepository = glycoproteinRepository;
		this.siteRepository = siteRepository;
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
	public void addTagToGlycoproteins(Collection<Glycoprotein> glycoproteins, String tag, UserEntity user) {
		if (glycoproteins != null) {
			GlycanTag gTag = new GlycanTag();
    		gTag.setLabel(tag);
    		gTag.setUser(user);
    		GlycanTag existing = glycanTagRepository.findByUserAndLabel(user, tag);
    		if (existing == null) {
    			existing = glycanTagRepository.save(gTag);
    		}
        	for (Glycoprotein g: glycoproteins) {
        		if (!g.hasTag(existing.getLabel())) {
        			g.addTag (existing);
        			glycoproteinRepository.save(g);
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
	public Glycoprotein addUploadToGlycoprotein (Glycoprotein gp, BatchUploadEntity upload, Boolean isNew, UserEntity user) {
		if (gp != null) {
			GlycoproteinInFile u = new GlycoproteinInFile();
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
            u.setGlycoprotein(gp);
            u.setIsNew(isNew);
            if (gp.getUploadFiles() == null) 
            	gp.setUploadFiles(new ArrayList<>());
            gp.getUploadFiles().add(u);
            Glycoprotein added = glycoproteinRepository.save(gp);
            upload.getGlycoproteins().add(u);
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
