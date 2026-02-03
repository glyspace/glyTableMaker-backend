package org.glygen.tablemaker.view;

import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.view.validation.EmailWithTld;
import org.glygen.tablemaker.view.validation.Password;
import org.glygen.tablemaker.view.validation.Username;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class User {
	private String userName;
	private String password;
	private String firstName;
    private String lastName;
    private String email;
    private String groupName;
    private String department;
    private String affiliation;
    private String affiliationWebsite;
    private Boolean tempPassword = false;
    private String userType = UserEntity.INVESTIGATOR;
    
	/**
	 * @return the userName
	 */
    @NotEmpty
    @Username
    @Size(min=5, max=20, message="userName should have atleast 5 and at most 20 characters")
	public String getUserName() {
		return userName;
	}
	/**
	 * @param userName the userName to set
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}
	/**
	 * @return the password
	 */
	@NotEmpty
	@Password(message="min 5, max 30 characters, at least 1 lowercase, 1 uppercase letter, 1 numeric and 1 special character")
	public String getPassword() {
		return password;
	}
	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}
	/**
	 * @return the firstName
	 */
	@Size(max=100, message="First name cannot exceed 100 characters")
	public String getFirstName() {
		return firstName;
	}
	/**
	 * @param firstName the firstName to set
	 */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	/**
	 * @return the lastName
	 */
	@Size(max=100, message="Last name cannot exceed 100 characters")
	public String getLastName() {
		return lastName;
	}
	/**
	 * @param lastName the lastName to set
	 */
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	/**
	 * @return the email
	 */
	@NotEmpty
	@EmailWithTld
	public String getEmail() {
		return email;
	}
	/**
	 * @param email the email to set
	 */
	public void setEmail(String email) {
		this.email = email;
	}
	/**
	 * @return the affiliation
	 */
	@Size(max=255, message="Affiliation cannot exceed 255 characters")
	public String getAffiliation() {
		return affiliation;
	}
	/**
	 * @param affiliation the affiliation to set
	 */
	public void setAffiliation(String affiliation) {
		this.affiliation = affiliation;
	}
	/**
	 * @return the affiliationWebsite
	 */
	@Size(max=255, message="Affiliation Website cannot exceed 255 characters")
	public String getAffiliationWebsite() {
		return affiliationWebsite;
	}
	/**
	 * @param affiliationWebsite the affiliationWebsite to set
	 */
	public void setAffiliationWebsite(String affiliationWebsite) {
		this.affiliationWebsite = affiliationWebsite;
	}
	/**
	 * @return the tempPassword flag
	 */
	public Boolean getTempPassword() {
		return tempPassword;
	}
	/**
	 * @param tempPassword the tempPassword flag to set
	 */
	public void setTempPassword(Boolean t) {
		this.tempPassword = t;
	}
	
	/**
	 * 
	 * @return the user type: curator, investigator etc
	 */
	public String getUserType() {
		return userType;
	}
	
	/**
	 * 
	 * @param userType the userType to set
	 */
	public void setUserType(String userType) {
		this.userType = userType;
	}
    /**
     * @return the groupName
     */
	@Size(max=255, message="Group name cannot exceed 255 characters")
    public String getGroupName() {
        return groupName;
    }
    /**
     * @param groupName the groupName to set
     */
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
    /**
     * @return the department
     */
    @Size(max=255, message="Department cannot exceed 255 characters")
    public String getDepartment() {
        return department;
    }
    /**
     * @param department the department to set
     */
    public void setDepartment(String department) {
        this.department = department;
    }

}
