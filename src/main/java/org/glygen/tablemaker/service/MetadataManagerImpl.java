package org.glygen.tablemaker.service;

import java.util.ArrayList;
import java.util.List;

import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.dao.DatatypeCategoryRepository;
import org.glygen.tablemaker.persistence.dao.DatatypeRepository;
import org.glygen.tablemaker.persistence.dao.MetadataRepository;
import org.glygen.tablemaker.persistence.glycan.Datatype;
import org.glygen.tablemaker.persistence.glycan.DatatypeCategory;
import org.glygen.tablemaker.persistence.glycan.DatatypeCategoryPK;
import org.glygen.tablemaker.persistence.glycan.DatatypeInCategory;
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
	public Datatype addDatatypeToCategory(Datatype d, DatatypeCategory cat, Boolean mandatory) {
		if (cat.getDataTypes() == null) {
			cat.setDataTypes(new ArrayList<>());
		}
		Datatype saved = datatypeRepository.save(d);
    	saved.setUri(d.getUri()+saved.getDatatypeId());
    	datatypeRepository.save(saved);
    	boolean exists = false;
    	for (DatatypeInCategory dc: cat.getDataTypes()) {
    		if (dc.getDatatype().equals(saved)) {
    			exists = true;
    			break;
    		}
    	}
    	if (!exists) {
    		DatatypeInCategory dc = new DatatypeInCategory();
    		dc.setDatatype(saved);
    		dc.setCategory(cat);
    		dc.setMandatory(mandatory);
    		DatatypeCategoryPK id = new DatatypeCategoryPK();
    		id.setCategory(cat);
    		id.setDatatype(d);
    		dc.setId(id);
    		cat.getDataTypes().add(dc);
    		datatypeCategoryRepository.save(cat);
    	}
		return saved;
	}

	@Override
	public void deleteDatatypeCategory(DatatypeCategory cat) {
		if (cat != null) {
			cat.getDataTypes().clear();
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
		List<DatatypeCategory> categories = datatypeCategoryRepository.findByDataTypes_datatype_datatypeId(dat.getDatatypeId());
		for (DatatypeCategory cat: categories) {
			List<DatatypeInCategory> toRemove = new ArrayList<>();
			for (DatatypeInCategory dc: cat.getDataTypes()) {
	    		if (dc.getDatatype().equals(dat)) {
	    			toRemove.add(dc);
	    		}
			}
			cat.getDataTypes().removeAll(toRemove);
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
