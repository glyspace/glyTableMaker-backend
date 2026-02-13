package org.glygen.tablemaker.persistence.dao;

import java.util.Optional;

import org.glygen.tablemaker.persistence.SoftwareEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SoftwareRepository extends JpaRepository<SoftwareEntity, Long> {
	
	@Query("SELECT s FROM SoftwareEntity s WHERE s.user.username = :username")
	Optional<SoftwareEntity> findByUsername(String username);

}
