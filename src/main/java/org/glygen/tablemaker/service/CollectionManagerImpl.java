package org.glygen.tablemaker.service;

import org.glygen.tablemaker.persistence.dao.CollectionRepository;
import org.glygen.tablemaker.persistence.dao.MetadataRepository;
import org.glygen.tablemaker.persistence.glycan.Collection;
import org.glygen.tablemaker.persistence.glycan.Metadata;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Transactional
@Service
public class CollectionManagerImpl implements CollectionManager {
	
	final private CollectionRepository collectionRepository;
	final private MetadataRepository metadataRepository;
	
	public CollectionManagerImpl(MetadataRepository metadataRepository, CollectionRepository collectionRepository) {
		this.collectionRepository = collectionRepository;
		this.metadataRepository = metadataRepository;
	}
	
	@Override
	public Collection saveCollectionWithMetadata(Collection c) {
		if (c.getMetadata() != null) {
			for (Metadata m: c.getMetadata()) {
				metadataRepository.save(m);
			}
		}
		return collectionRepository.save(c);
	}

}
