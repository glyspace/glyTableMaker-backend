package org.glygen.tablemaker.persistence.dao;

import java.util.Optional;

import org.glygen.tablemaker.persistence.GlycanImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GlycanImageRepository extends JpaRepository<GlycanImageEntity, Long> {
	
	Optional<GlycanImageEntity> findByGlycanId (Long glycanId);

}
