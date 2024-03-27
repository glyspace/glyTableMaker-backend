package org.glygen.tablemaker.service;

import java.util.Collection;
import java.util.List;

import org.glygen.tablemaker.persistence.BatchUploadEntity;
import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.dao.BatchUploadRepository;
import org.glygen.tablemaker.persistence.dao.GlycanRepository;
import org.glygen.tablemaker.persistence.dao.GlycanTagRepository;
import org.glygen.tablemaker.persistence.glycan.Glycan;
import org.glygen.tablemaker.persistence.glycan.GlycanTag;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Transactional
@Service
public class GlycanManagerImpl implements GlycanManager {

	final private GlycanRepository glycanRepository;
	final private GlycanTagRepository glycanTagRepository;
	final private BatchUploadRepository uploadRepository;
    
    public GlycanManagerImpl(GlycanTagRepository glycanTagRepository, GlycanRepository glycanRepository, BatchUploadRepository uploadRepository) {
		this.glycanRepository = glycanRepository;
		this.glycanTagRepository = glycanTagRepository;
		this.uploadRepository = uploadRepository;
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
        		g.addTag (existing);
        		glycanRepository.save(g);
        	}
        }
	}
	@Override
	public void deleteUploadEntity(BatchUploadEntity upload, UserEntity user) {
		List<Glycan> glycans = glycanRepository.findByUserAndUploadFiles_Id(user, upload.getId());
		for (Glycan g: glycans) {
			g.removeUploadFile (upload);
			glycanRepository.save(g);
		}
		uploadRepository.deleteById(upload.getId());
	}
}
