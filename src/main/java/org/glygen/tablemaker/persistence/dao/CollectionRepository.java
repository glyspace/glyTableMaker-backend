package org.glygen.tablemaker.persistence.dao;

import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.glycan.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CollectionRepository extends JpaRepository<Collection, Long>, JpaSpecificationExecutor<Collection> {
	public Page<Collection> findAllByUser(UserEntity user, Pageable pageable);
	public Collection findByCollectionIdAndUser (Long id, UserEntity user);
}
