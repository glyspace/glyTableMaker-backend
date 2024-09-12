package org.glygen.tablemaker.persistence.dao;

import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.dataset.Dataset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DatasetRepository extends JpaRepository<Dataset, Long>, JpaSpecificationExecutor<Dataset> {
	
	public Page<Dataset> findAllByUser(UserEntity user, Pageable pageable);
	public Page<Dataset> findAll(Specification<Dataset> spec, Pageable pageable);
	public long countByDatasetIdentifier (String identifier);
	public Dataset findByDatasetIdentifierAndUserAndVersions_version (String identifier, UserEntity user, String version);

}
