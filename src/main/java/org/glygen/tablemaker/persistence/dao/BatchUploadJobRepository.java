package org.glygen.tablemaker.persistence.dao;

import org.glygen.tablemaker.persistence.BatchUploadJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchUploadJobRepository extends JpaRepository<BatchUploadJob, Long> {

}
