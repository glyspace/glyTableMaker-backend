package org.glygen.tablemaker.persistence.dao;

import org.glygen.tablemaker.persistence.SearchResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchResultRepository extends JpaRepository<SearchResultEntity, String> {
	SearchResultEntity findBySearchKey (String searchKey);
}