package org.glygen.tablemaker.persistence.dao;

import java.util.List;

import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.glycan.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CollectionRepository extends JpaRepository<Collection, Long>, JpaSpecificationExecutor<Collection> {
	public Page<Collection> findAllByUser(UserEntity user, Pageable pageable);
	public Collection findByCollectionIdAndUser (Long id, UserEntity user);
	public List<Collection> findAllByNameAndUser(String name, UserEntity user);
	
	@Query("SELECT c FROM Collection c WHERE (c.collections is not empty and c.user = :user)")
	public List<Collection> findParentCollectionsByUser (@Param("user") UserEntity user);
	
	@Query("SELECT c FROM Collection c WHERE (c.collections is not empty and c.user = :user)")
	public Page<Collection> findParentCollectionsByUser (@Param("user") UserEntity user, Pageable pageable);
	
	@Query("SELECT c FROM Collection c WHERE (c.collections is empty and c.user = :user)")
	public Page<Collection> findNonParentCollectionsByUser (@Param("user") UserEntity user, Pageable pageable);
}
