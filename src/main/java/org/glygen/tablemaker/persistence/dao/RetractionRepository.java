package org.glygen.tablemaker.persistence.dao;

import java.util.Optional;

import org.glygen.tablemaker.persistence.dataset.Dataset;
import org.glygen.tablemaker.persistence.dataset.Retraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RetractionRepository extends JpaRepository<Retraction, Long> {
	Optional<Retraction> findByDataset(Dataset dataset);
	
	@Query("SELECT r FROM Retraction r WHERE r.dataset.datasetId = :datasetId")
	Optional<Retraction> findByDatasetId(Long datasetId);
}
