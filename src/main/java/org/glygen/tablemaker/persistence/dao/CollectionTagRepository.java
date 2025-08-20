package org.glygen.tablemaker.persistence.dao;

import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.glycan.CollectionTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectionTagRepository extends JpaRepository<CollectionTag, Long> {
	CollectionTag findByUserAndLabel(UserEntity user, String label);
}
