package org.glygen.tablemaker.persistence.dao;

import org.glygen.tablemaker.persistence.UploadErrorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadErrorRepository extends JpaRepository<UploadErrorEntity, Long> {

}
