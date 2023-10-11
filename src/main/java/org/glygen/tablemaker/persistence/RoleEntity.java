package org.glygen.tablemaker.persistence;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

@Entity(name="roles")
@XmlRootElement (name="role")
@JsonSerialize
public class RoleEntity {
	
	public final static String ADMIN="ADMIN";
	public final static String USER="USER";
	public final static String MODERATOR="MODERATOR";
	public final static String DATAENTRY="DATA";
	
	Integer roleId;
	String roleName;
	Set <UserEntity> users;
	
	public RoleEntity() {
	}
	
	public RoleEntity (String role) {
		this.roleName=role;
	}
	
	/**
	 * @return the roleId
	 */
	@XmlAttribute
	@Id
	@Column(name="roleid")
	@GeneratedValue (strategy=GenerationType.SEQUENCE, generator="role_seq")
	@SequenceGenerator(name="role_seq", sequenceName="ROLE_SEQ", initialValue=4, allocationSize=50)
	public Integer getRoleId() {
		return roleId;
	}
	
	/**
	 * @param roleId the roleId to set
	 */
	public void setRoleId(Integer roleId) {
		this.roleId = roleId;
	}
	
	/**
	 * @return the roleName
	 */
	@Column(name="name", nullable=false, unique=true)
	@XmlAttribute
	public String getRoleName() {
		return roleName;
	}
	
	/**
	 * @param roleName the roleName to set
	 */
	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}
	
	/**
	 * @return the users
	 */
	@ManyToMany(fetch=FetchType.EAGER, mappedBy = "roles")
	@XmlTransient  // so that from the role we should not go back to users - prevent cycles
	@JsonIgnore
	public Set<UserEntity> getUsers() {
		return users;
	}
	
	/**
	 * @param users the users to set
	 */
	public void setUsers(Set<UserEntity> users) {
		this.users = users;
	}
	
	public void addUser (UserEntity user) {
		if (this.users == null)
			this.users = new HashSet<>();
		this.users.add(user);
	}
}
