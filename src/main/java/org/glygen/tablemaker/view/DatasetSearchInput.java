package org.glygen.tablemaker.view;

public class DatasetSearchInput {
	
	String datasetName;
	String username;
	String institution;
	String department;
	String group;
	String license;
	String type; // glycomics or glycoproteomics
	String paperId;
	String paperAuthor;
	String fundingAgency;
	
	public String getDatasetName() {
		return datasetName;
	}
	public void setDatasetName(String datasetName) {
		this.datasetName = datasetName;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getInstitution() {
		return institution;
	}
	public void setInstitution(String institution) {
		this.institution = institution;
	}
	public String getDepartment() {
		return department;
	}
	public void setDepartment(String department) {
		this.department = department;
	}
	public String getGroup() {
		return group;
	}
	public void setGroup(String group) {
		this.group = group;
	}
	public String getLicense() {
		return license;
	}
	public void setLicense(String license) {
		this.license = license;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getPaperId() {
		return paperId;
	}
	public void setPaperId(String paperId) {
		this.paperId = paperId;
	}
	public String getPaperAuthor() {
		return paperAuthor;
	}
	public void setPaperAuthor(String paperAuthor) {
		this.paperAuthor = paperAuthor;
	}
	public String getFundingAgency() {
		return fundingAgency;
	}
	public void setFundingAgency(String fundingAgency) {
		this.fundingAgency = fundingAgency;
	}
}
