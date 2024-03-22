package org.glygen.tablemaker.persistence.dao;

import java.util.Collection;

import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.glycan.GlycanTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GlycanTagRepository extends JpaRepository<GlycanTag, Long> {
	GlycanTag findByUserAndLabel(UserEntity user, String label);
	Collection<GlycanTag> findAllByUser (UserEntity user);
}
