package org.glygen.tablemaker.persistence.dao;

import java.util.Optional;

import org.glygen.tablemaker.persistence.dataset.Dataset;
import org.glygen.tablemaker.persistence.dataset.Retraction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RetractionRepository extends JpaRepository<Retraction, Long> {
	Optional<Retraction> findByDataset(Dataset dataset);
}
