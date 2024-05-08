package org.glygen.tablemaker.persistence.dao;

import org.glygen.tablemaker.persistence.glycan.Metadata;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MetadataRepository extends JpaRepository<Metadata, Long> {

}
