package org.glygen.tablemaker.persistence.dao;

import java.util.List;

import org.glygen.tablemaker.persistence.BatchUploadEntity;
import org.glygen.tablemaker.persistence.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchUploadRepository extends JpaRepository<BatchUploadEntity, Long> {
	List<BatchUploadEntity> findByUser (UserEntity user);
}
