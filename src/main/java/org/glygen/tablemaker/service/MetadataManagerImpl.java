package org.glygen.tablemaker.service;

import java.util.ArrayList;
import java.util.List;

import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.dao.DatatypeCategoryRepository;
import org.glygen.tablemaker.persistence.dao.DatatypeRepository;
import org.glygen.tablemaker.persistence.dao.MetadataRepository;
import org.glygen.tablemaker.persistence.glycan.Datatype;
import org.glygen.tablemaker.persistence.glycan.DatatypeCategory;
import org.glygen.tablemaker.persistence.glycan.Glycan;
import org.glygen.tablemaker.persistence.glycan.Metadata;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Transactional
@Service
public class MetadataManagerImpl implements MetadataManager {
	
	final private DatatypeRepository datatypeRepository;
	final private DatatypeCategoryRepository datatypeCategoryRepository;
	final private MetadataRepository metadataRepository;
	
	public MetadataManagerImpl(DatatypeRepository datatypeRepository, DatatypeCategoryRepository datatypeCategoryRepository, MetadataRepository metadataRepository) {
		this.datatypeRepository = datatypeRepository;
		this.datatypeCategoryRepository = datatypeCategoryRepository;
		this.metadataRepository = metadataRepository;
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
			// delete datatypes
	        for (Datatype d: cat.getDataTypes()) {
	        	datatypeRepository.delete(d);
	        }
	        datatypeCategoryRepository.deleteById(cat.getCategoryId());
		}
		
	}
	
	@Override
	public List<Metadata> getMetadata (Datatype dat) {
		return metadataRepository.findByType_datatypeId(dat.getDatatypeId());
	}
	
	@Override
	public void deleteDatatype(Datatype dat) {
		// find the category for the datatype
		List<DatatypeCategory> categories = datatypeCategoryRepository.findByDataTypes_datatypeId(dat.getDatatypeId());
		for (DatatypeCategory cat: categories) {
			cat.getDataTypes().remove(dat);
			datatypeCategoryRepository.save(cat);
		}
		// find metadata using this datatype and delete them as well
		List<Metadata> metadata = getMetadata(dat);
		for (Metadata m: metadata) {
			metadataRepository.delete(m);
		}
		datatypeRepository.deleteById(dat.getDatatypeId());
	}
}
