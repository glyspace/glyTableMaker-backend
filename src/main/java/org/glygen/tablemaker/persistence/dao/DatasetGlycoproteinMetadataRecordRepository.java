package org.glygen.tablemaker.persistence.dao;

import org.glygen.tablemaker.persistence.dataset.DatasetGlycoproteinMetadataRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DatasetGlycoproteinMetadataRecordRepository extends JpaRepository<DatasetGlycoproteinMetadataRecord, Long>, 
		JpaSpecificationExecutor<DatasetGlycoproteinMetadataRecord> {

    Page<DatasetGlycoproteinMetadataRecord> findByDatasetVersionId(Long versionId, Pageable pageable);
    Page<DatasetGlycoproteinMetadataRecord> findByDatasetDatasetDatasetIdentifierAndDatasetHeadTrue(String datasetIdentifier,Pageable pageable);
    Page<DatasetGlycoproteinMetadataRecord> findAll(Specification<DatasetGlycoproteinMetadataRecord> spec, Pageable pageable);
}

