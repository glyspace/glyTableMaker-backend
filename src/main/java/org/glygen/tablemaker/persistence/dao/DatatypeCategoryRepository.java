package org.glygen.tablemaker.persistence.dao;

import java.util.List;

import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.glycan.DatatypeCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatatypeCategoryRepository extends JpaRepository<DatatypeCategory, Long> {
	DatatypeCategory findByNameIgnoreCaseAndUser (String name, UserEntity user);
	List<DatatypeCategory> findByDataTypes_datatype_datatypeId (Long datatypeId);
}
