package org.glygen.tablemaker.persistence.dao;

import java.util.Optional;

import org.glygen.tablemaker.persistence.ApplicationSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationSettingsRepository extends JpaRepository<ApplicationSettingsEntity, String> {
	Optional<ApplicationSettingsEntity> findByName (String name);
}
