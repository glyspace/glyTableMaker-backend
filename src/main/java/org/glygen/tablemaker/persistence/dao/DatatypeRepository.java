package org.glygen.tablemaker.persistence.dao;

import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.glycan.Datatype;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatatypeRepository extends JpaRepository<Datatype, Long> {
	Datatype findByNameIgnoreCaseAndUser (String name, UserEntity user);
}
