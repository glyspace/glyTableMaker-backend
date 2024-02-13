package org.glygen.tablemaker.persistence.dao;

import org.glygen.tablemaker.persistence.BatchUploadEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchUploadRepository extends JpaRepository<BatchUploadEntity, Long> {

}
