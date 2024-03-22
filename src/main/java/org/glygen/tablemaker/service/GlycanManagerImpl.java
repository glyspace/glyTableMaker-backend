package org.glygen.tablemaker.service;

import java.util.Collection;

import org.glygen.tablemaker.persistence.UserEntity;
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
    
    public GlycanManagerImpl(GlycanTagRepository glycanTagRepository, GlycanRepository glycanRepository) {
		this.glycanRepository = glycanRepository;
		this.glycanTagRepository = glycanTagRepository;
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
}
