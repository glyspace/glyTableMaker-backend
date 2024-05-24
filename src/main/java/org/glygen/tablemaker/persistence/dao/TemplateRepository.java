package org.glygen.tablemaker.persistence.dao;

import java.util.List;

import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.table.TableMakerTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateRepository extends JpaRepository<TableMakerTemplate, Long> {
	TableMakerTemplate findByNameIgnoreCaseAndUser (String name, UserEntity user);
	public List<TableMakerTemplate> findAllByUser(UserEntity user);
}
