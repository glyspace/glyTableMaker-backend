package org.glygen.tablemaker.persistence.dao;

import org.glygen.tablemaker.persistence.dataset.Publication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PublicationRepositoryCustom {
	
	Page<Publication> listPublications(String datasetIdentifier, String globalFilter, Pageable pageable);

	Page<Publication> listPublications(Long versionId, String globalFilter, Pageable pageable);

}
