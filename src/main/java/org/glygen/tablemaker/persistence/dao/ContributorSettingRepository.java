package org.glygen.tablemaker.persistence.dao;

import java.util.List;

import org.glygen.tablemaker.persistence.ContributorSetting;
import org.glygen.tablemaker.persistence.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContributorSettingRepository extends JpaRepository<ContributorSetting, Long> {
	List<ContributorSetting> findAllByUser (UserEntity user);
	List<ContributorSetting> findAllByUserAndDefaultSetting (UserEntity user, Boolean defaultSetting);
}
