package org.glygen.tablemaker.persistence.dao;

import java.util.List;
import java.util.Optional;

import org.glygen.tablemaker.persistence.SettingEntity;
import org.glygen.tablemaker.persistence.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingRepository extends JpaRepository<SettingEntity, String> {
	List<SettingEntity> findAllByUser (UserEntity user);
	Optional<SettingEntity> findByNameAndUser (String name, UserEntity user);
}
