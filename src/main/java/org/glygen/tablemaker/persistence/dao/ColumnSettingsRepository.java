package org.glygen.tablemaker.persistence.dao;

import java.util.List;

import org.glygen.tablemaker.persistence.ColumnVisibillitySetting;
import org.glygen.tablemaker.persistence.TableMakerTable;
import org.glygen.tablemaker.persistence.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ColumnSettingsRepository extends JpaRepository<ColumnVisibillitySetting, Long> {
	List<ColumnVisibillitySetting> findByTableNameAndUser (TableMakerTable tableName, UserEntity user);
	List<ColumnVisibillitySetting> findAllByUser (UserEntity user);
}
