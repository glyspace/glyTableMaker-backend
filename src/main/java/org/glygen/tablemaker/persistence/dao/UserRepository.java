package org.glygen.tablemaker.persistence.dao;

import java.util.List;

import org.glygen.tablemaker.persistence.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

	public UserEntity findByEmailIgnoreCase(String email);
	public UserEntity findByUsernameIgnoreCase(String username);
	public List<UserEntity> findAllByLastNameIgnoreCase (String lastName);
	public List<UserEntity> findAllByFirstNameIgnoreCase (String firstName);
	public List<UserEntity> findAllByGroupNameIgnoreCase (String groupName);
	public List<UserEntity> findAllByAffiliationIgnoreCase (String affiliation);
	public List<UserEntity> findAllByDepartmentIgnoreCase (String department);
	
	@Query("SELECT e FROM UserEntity e ORDER BY e.lastLoginDate DESC NULLS LAST")
	List<UserEntity> findAllOrderByLastLoginDateDescNullsLast();
}
