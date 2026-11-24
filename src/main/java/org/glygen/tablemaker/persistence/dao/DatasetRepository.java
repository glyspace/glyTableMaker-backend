package org.glygen.tablemaker.persistence.dao;

import java.util.List;

import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.dataset.Dataset;
import org.glygen.tablemaker.persistence.dataset.DatasetProjection;
import org.glygen.tablemaker.persistence.dataset.License;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DatasetRepository extends JpaRepository<Dataset, Long>, JpaSpecificationExecutor<Dataset> {
	
    Page<DatasetProjection> findAllBy(Pageable pageable);
	Page<DatasetProjection> findAllByUser (UserEntity user, Pageable pageable);
	Page<Dataset> findAll(Specification<Dataset> spec, Pageable pageable);
	long countByDatasetIdentifier (String identifier);
	long countByUser (UserEntity user);
	long countByRetractedIsNullOrRetractedFalse();
	Dataset findByDatasetIdentifierAndUserAndVersions_version (String identifier, UserEntity user, String version);
	Dataset findByDatasetIdentifierAndUserAndVersions_head (String identifier, UserEntity user, Boolean head);
	Dataset findByDatasetIdentifierAndVersions_version (String identifier, String version);
	Dataset findByDatasetIdentifierAndVersions_head (String identifier, Boolean head);
	Dataset findByDatasetIdAndUser (Long id, UserEntity user);
	Dataset findByDatasetId(Long id);
	List<Dataset> findAllByNameAndUser (String name, UserEntity user);
	List<Dataset> findByNameContainingIgnoreCase (String name);
	
	@Query("select count(distinct r.uniProtId) " +
		       "from DatasetVersion dv join dv.glycoproteinRecords r WHERE dv.dataset.datasetId = :datasetId AND dv.head = true")
	long getProteinCount (@Param("datasetId")Long datasetId);
	
	@Query("select count(distinct r.uniProtId) " +
		       "from DatasetVersion dv join dv.glycoproteinRecords r WHERE dv.version = :version")
	long getProteinCountByVersion (@Param("version")String version);
	
	@Query("select count(distinct d.user)from Dataset d where d.retracted is null or d.retracted = false")
	long countUsersByDataset();
	
	@Query("select count(distinct r.glytoucanId) " +
		       "from DatasetVersion dv join dv.records r " +
			"WHERE dv.dataset.datasetId = :datasetId AND dv.head = true")
    long getGlycanCount (@Param("datasetId")Long datasetId);
	
	@Query("select count(distinct r.glytoucanId) " +
		       "from DatasetVersion dv join dv.records r " +
		       "where dv.head = true " +
		       "and (dv.dataset.retracted is null or dv.dataset.retracted = false)")
	long getAllGlycanCount();
	
	@Query("select count(distinct r.uniProtId) " +
		       "from DatasetVersion dv join dv.glycoproteinRecords r " +
		       "where dv.head = true " +
		       "and (dv.dataset.retracted is null or dv.dataset.retracted = false)")
	long getAllGlycoproteinCount();
	
	@Query("select count(g)from DatasetMetadataRecord g where g.dataset.head = true and g.dataset.dataset.datasetId = :datasetId")
	public long getGlycanMetadataCount(@Param("datasetId")Long datasetId);
	
	@Query("select count(g) from DatasetGlycoproteinMetadataRecord g where g.dataset.head = true and g.dataset.dataset.datasetId = :datasetId")
	public long getGlycoproteinMetadataCount(@Param("datasetId")Long datasetId);
	
	@Query("select count(distinct r.glytoucanId) " +
		       "from DatasetVersion dv join dv.records r  WHERE dv.head = true and (dv.dataset.retracted is null or dv.dataset.retracted = false)")
	public List<String> getAllPublicGlytoucanIds ();
	
	@Query("Select DISTINCT d.datasetId FROM Dataset d WHERE d.user = :user")
	public List<Long> getAllDatasetIdsByUser (@Param("user") UserEntity user);
	
	@Query("SELECT dv.license FROM DatasetVersion dv WHERE dv.dataset.datasetId = :datasetId AND dv.head = true")
	public License getLicenseByDatasetId (@Param("datasetId") Long datasetId);
	
	@Query("SELECT dv.version FROM DatasetVersion dv WHERE dv.dataset.datasetId = :datasetId AND dv.head = true")
	public String getLatestVersionByDatasetId (@Param("datasetId") Long datasetId);
	
	@Query("SELECT dv.versionId FROM DatasetVersion dv WHERE dv.dataset.datasetIdentifier = :datasetId AND dv.head = true")
	public Long getLatestVersionIdByDatasetIdentifier (@Param("datasetId") String datasetIdentifier);
	
	@Query("SELECT dv.versionDate FROM DatasetVersion dv WHERE dv.dataset.datasetId = :datasetId AND dv.head = true")
	public java.util.Date getLatestVersionDateByDatasetId (@Param("datasetId") Long datasetId);
	
	@Query("Select DISTINCT d.datasetId FROM Dataset d ")
	public List<Long> getAllDatasetIds ();
	
	@Query("Select DISTINCT d.name FROM Dataset d ")
	public List<String> getAllDatasetNames ();
	
	@Query("SELECT DISTINCT g.fundingOrganization FROM Grant g")
	public List<String> getAllFundingOrganizations ();
	
	public Page<Dataset> findByDatasetIdIn(Iterable<Long> ids, Pageable pageable);
	
	@Query("Select DISTINCT d.datasetId FROM Dataset d JOIN d.grants g WHERE LOWER(g.fundingOrganization) = :fundingOrg")
	public List<Long> getAllDatasetIdsByFundingOrganization (@Param("fundingOrg") String fundingOrg);
	
	
	@Query("Select DISTINCT d.datasetId FROM Dataset d JOIN d.integratedIn g WHERE LOWER(g.resource.name) = :resource")
	List<Dataset> getDatasetsIntegratedIn (@Param("resource")String resourceName);
	
	@Query("""
	    SELECT g.errorJson
	    FROM DatabaseResourceDataset g
	    WHERE LOWER(g.resource.name) = 'glygen'
	      AND g.dataset.datasetId = :datasetId
	      AND g.errorJson IS NOT NULL
	    ORDER BY g.id DESC
	    """)
	List<String> findGlygenErrors(@Param("datasetId") Long datasetId, Pageable pageable);
	default String getGlygenErrors (@Param("datasetId") Long datasetId) {
		return findGlygenErrors(datasetId, PageRequest.of(0, 1)).stream().findFirst().orElse(null);
	}
	
}
