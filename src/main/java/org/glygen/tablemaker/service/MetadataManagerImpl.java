package org.glygen.tablemaker.service;

import java.util.ArrayList;

import org.glygen.tablemaker.persistence.dao.DatatypeCategoryRepository;
import org.glygen.tablemaker.persistence.dao.DatatypeRepository;
import org.glygen.tablemaker.persistence.glycan.Datatype;
import org.glygen.tablemaker.persistence.glycan.DatatypeCategory;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Transactional
@Service
public class MetadataManagerImpl implements MetadataManager {
	
	final private DatatypeRepository datatypeRepository;
	final private DatatypeCategoryRepository datatypeCategoryRepository;
	
	public MetadataManagerImpl(DatatypeRepository datatypeRepository, DatatypeCategoryRepository datatypeCategoryRepository) {
		this.datatypeRepository = datatypeRepository;
		this.datatypeCategoryRepository = datatypeCategoryRepository;
	}

	@Override
	public Datatype addDatatypeToCategory(Datatype d, DatatypeCategory cat) {
		if (cat.getDataTypes() == null) {
			cat.setDataTypes(new ArrayList<>());
		}
		Datatype saved = datatypeRepository.save(d);
    	saved.setUri(d.getUri()+saved.getDatatypeId());
    	datatypeRepository.save(saved);
		cat.getDataTypes().add(saved);
		datatypeCategoryRepository.save(cat);
		return saved;
	}

	@Override
	public void deleteDatatypeCategory(DatatypeCategory cat) {
		if (cat != null) {
			// delete datatypes, TODO how to delete from datatype_category table??
	        for (Datatype d: cat.getDataTypes()) {
	        	datatypeRepository.delete(d);
	        }
	        datatypeCategoryRepository.deleteById(cat.getCategoryId());
		}
		
	}
}
