package org.glygen.tablemaker.persistence.dao;

import java.util.List;

import org.glygen.tablemaker.persistence.BatchUploadEntity;
import org.glygen.tablemaker.persistence.BatchUploadJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchUploadJobRepository extends JpaRepository<BatchUploadJob, Long> {
	
	List<BatchUploadJob> findAllByUpload (BatchUploadEntity upload);

}
