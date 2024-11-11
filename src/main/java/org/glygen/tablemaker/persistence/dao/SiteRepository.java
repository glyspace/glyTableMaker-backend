package org.glygen.tablemaker.persistence.dao;

import org.glygen.tablemaker.persistence.protein.Site;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteRepository extends JpaRepository<Site, Long> {

}
