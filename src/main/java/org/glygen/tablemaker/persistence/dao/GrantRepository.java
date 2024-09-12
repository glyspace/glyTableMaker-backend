package org.glygen.tablemaker.persistence.dao;

import org.glygen.tablemaker.persistence.dataset.Grant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrantRepository extends JpaRepository<Grant, Long> {

}
