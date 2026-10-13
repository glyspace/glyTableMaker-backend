package org.glygen.tablemaker.view.dto;

import org.glygen.tablemaker.persistence.UserEntity;

public class UserDTO {

	private String userName;
	private String firstName;
    private String lastName;
    private String groupName;
    private String department;
    private String researchCenter;
    private String affiliation;
    private String affiliationWebsite;
    private String userType = UserEntity.CURATOR;
    
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	public String getGroupName() {
		return groupName;
	}
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}
	public String getDepartment() {
		return department;
	}
	public void setDepartment(String department) {
		this.department = department;
	}
	public String getResearchCenter() {
		return researchCenter;
	}
	public void setResearchCenter(String researchCenter) {
		this.researchCenter = researchCenter;
	}
	public String getAffiliation() {
		return affiliation;
	}
	public void setAffiliation(String affiliation) {
		this.affiliation = affiliation;
	}
	public String getAffiliationWebsite() {
		return affiliationWebsite;
	}
	public void setAffiliationWebsite(String affiliationWebsite) {
		this.affiliationWebsite = affiliationWebsite;
	}
	public String getUserType() {
		return userType;
	}
	public void setUserType(String userType) {
		this.userType = userType;
	}
}
