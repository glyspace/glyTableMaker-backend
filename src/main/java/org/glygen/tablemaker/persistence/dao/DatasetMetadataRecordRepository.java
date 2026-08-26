package org.glygen.tablemaker.persistence.dao;

import org.glygen.tablemaker.persistence.dataset.DatasetMetadataRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DatasetMetadataRecordRepository extends JpaRepository<DatasetMetadataRecord, Long>, JpaSpecificationExecutor<DatasetMetadataRecord> {

    Page<DatasetMetadataRecord> findByDatasetVersionId(Long versionId, Pageable pageable);
    Page<DatasetMetadataRecord> findByDatasetDatasetDatasetIdentifierAndDatasetHeadTrue(String datasetIdentifier,Pageable pageable);
    Page<DatasetMetadataRecord> findAll(Specification<DatasetMetadataRecord> spec, Pageable pageable);
}
