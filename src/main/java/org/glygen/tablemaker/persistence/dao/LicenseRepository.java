package org.glygen.tablemaker.persistence.dao;

import org.glygen.tablemaker.persistence.dataset.License;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LicenseRepository extends JpaRepository<License, Long> {

}
