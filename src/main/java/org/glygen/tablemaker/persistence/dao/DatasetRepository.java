package org.glygen.tablemaker.persistence.dao;

import java.util.List;

import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.dataset.Dataset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DatasetRepository extends JpaRepository<Dataset, Long>, JpaSpecificationExecutor<Dataset> {
	
	public Page<Dataset> findAllByUser(UserEntity user, Pageable pageable);
	public Page<Dataset> findAll(Specification<Dataset> spec, Pageable pageable);
	public long countByDatasetIdentifier (String identifier);
	public Dataset findByDatasetIdentifierAndUserAndVersions_version (String identifier, UserEntity user, String version);
	public Dataset findByDatasetIdentifierAndUserAndVersions_head (String identifier, UserEntity user, Boolean head);
	public Dataset findByDatasetIdentifierAndVersions_version (String identifier, String version);
	public Dataset findByDatasetIdentifierAndVersions_head (String identifier, Boolean head);
	public Dataset findByDatasetIdAndUser (Long id, UserEntity user);
	public List<Dataset> findAllByNameAndUser (String name, UserEntity user);
	public List<Dataset> findByNameContainingIgnoreCase (String name);
	
	@Query("Select DISTINCT d.datasetId FROM Dataset d WHERE d.user = :user")
	public List<Long> getAllDatasetIdsByUser (@Param("user") UserEntity user);
	
	@Query("Select DISTINCT d.datasetId FROM Dataset d ")
	public List<Long> getAllDatasetIds ();
	
	@Query("Select DISTINCT d.name FROM Dataset d ")
	public List<String> getAllDatasetNames ();
	
	@Query("SELECT DISTINCT g.fundingOrganization FROM Grant g")
	public List<String> getAllFundingOrganizations ();
	
	public Page<Dataset> findByDatasetIdIn(Iterable<Long> ids, Pageable pageable);
	
	@Query("Select DISTINCT d.datasetId FROM Dataset d JOIN d.grants g WHERE LOWER(g.fundingOrganization) = :fundingOrg")
	public List<Long> getAllDatasetIdsByFundingOrganization (@Param("fundingOrg") String fundingOrg);

}
