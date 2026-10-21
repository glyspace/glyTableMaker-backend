package org.glygen.tablemaker.view.dto;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.glygen.tablemaker.persistence.dataset.DatabaseResource;
import org.glygen.tablemaker.persistence.dataset.DatabaseResourceDataset;
import org.glygen.tablemaker.persistence.dataset.Grant;
import org.glygen.tablemaker.persistence.dataset.Publication;
import org.glygen.tablemaker.persistence.dataset.Retraction;

import com.fasterxml.jackson.annotation.JsonFormat;

public class DatasetDTO {
	
	Long datasetId;
	String datasetIdentifier;
	String name;
	String description;
	String notes;
	Boolean retracted;
	Boolean removed;
	
	Retraction retraction;
	List<VersionInfo> versionList;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
	Date dateCreated;
	
	UserDTO user;
	
	Collection<Grant> grants;
	Collection<DatabaseResourceDataset> integratedIn;
	Collection<DatabaseResource> associatedDatasources;
	Collection<Publication> associatedPapers;
	DatasetVersionDTO version;
	
	String fileVersion = "1.0";
	
	public Long getDatasetId() {
		return datasetId;
	}
	public void setDatasetId(Long datasetId) {
		this.datasetId = datasetId;
	}
	public String getDatasetIdentifier() {
		return datasetIdentifier;
	}
	public void setDatasetIdentifier(String datasetIdentifier) {
		this.datasetIdentifier = datasetIdentifier;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getNotes() {
		return notes;
	}
	public void setNotes(String notes) {
		this.notes = notes;
	}
	public Boolean getRetracted() {
		return retracted;
	}
	public void setRetracted(Boolean retracted) {
		this.retracted = retracted;
	}
	public Date getDateCreated() {
		return dateCreated;
	}
	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}
	public UserDTO getUser() {
		return user;
	}
	public void setUser(UserDTO user) {
		this.user = user;
	}
	public Collection<Grant> getGrants() {
		return grants;
	}
	public void setGrants(Collection<Grant> grants) {
		this.grants = grants;
	}
	public Collection<DatabaseResourceDataset> getIntegratedIn() {
		return integratedIn;
	}
	public void setIntegratedIn(Collection<DatabaseResourceDataset> integratedIn) {
		this.integratedIn = integratedIn;
	}
	public Collection<DatabaseResource> getAssociatedDatasources() {
		return associatedDatasources;
	}
	public void setAssociatedDatasources(Collection<DatabaseResource> associatedDatasources) {
		this.associatedDatasources = associatedDatasources;
	}
	public Collection<Publication> getAssociatedPapers() {
		return associatedPapers;
	}
	public void setAssociatedPapers(Collection<Publication> associatedPapers) {
		this.associatedPapers = associatedPapers;
	}
	public void setVersion(DatasetVersionDTO version) {
		this.version = version;
	}
	public DatasetVersionDTO getVersion() {
		return version;
	}
	public String getFileVersion() {
		return fileVersion;
	}
	public void setFileVersion(String fileVersion) {
		this.fileVersion = fileVersion;
	}
	public Boolean getRemoved() {
		return removed;
	}
	public void setRemoved(Boolean removed) {
		this.removed = removed;
	}
	public Retraction getRetraction() {
		return retraction;
	}
	public void setRetraction(Retraction retraction) {
		this.retraction = retraction;
	}
	public List<VersionInfo> getVersionList() {
		return versionList;
	}
	public void setVersionList(List<VersionInfo> versionList) {
		this.versionList = versionList;
	}
}
