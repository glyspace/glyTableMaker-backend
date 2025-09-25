package org.glygen.tablemaker.persistence.dao;

import java.util.List;

import org.glygen.tablemaker.persistence.dataset.Publication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PublicationRepository extends JpaRepository<Publication, Long>, PublicationRepositoryCustom {
	
	List<Publication> findByDoiId (String doi);
	List<Publication> findByPubmedId (Integer pubmedId);
	
	@Query("SELECT p FROM DatasetVersion dv JOIN dv.publications p WHERE dv.versionId = :versionId")
	Page<Publication> findByDatasetVersionId(@Param("versionId") Long versionId, Pageable pageable);
	
	@Query("SELECT p FROM DatasetVersion dv JOIN dv.publications p WHERE dv.dataset.datasetIdentifier = :datasetId AND dv.head = true")
	Page<Publication> findPublicationsFromHeadVersion(@Param("datasetId") String datasetId, Pageable pageable);
}
