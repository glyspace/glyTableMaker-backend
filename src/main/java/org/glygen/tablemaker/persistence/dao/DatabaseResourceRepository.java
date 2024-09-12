package org.glygen.tablemaker.persistence.dao;

import org.glygen.tablemaker.persistence.dataset.DatabaseResource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatabaseResourceRepository extends JpaRepository<DatabaseResource, Long> {

}
