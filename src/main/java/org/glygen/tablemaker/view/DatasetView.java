package org.glygen.tablemaker.view;

import java.util.Date;
import java.util.List;

import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.dataset.DatabaseResource;
import org.glygen.tablemaker.persistence.dataset.DatabaseResourceDataset;
import org.glygen.tablemaker.persistence.dataset.DatasetMetadata;
import org.glygen.tablemaker.persistence.dataset.DatasetVersion;
import org.glygen.tablemaker.persistence.dataset.Grant;
import org.glygen.tablemaker.persistence.dataset.License;
import org.glygen.tablemaker.persistence.dataset.Publication;

public class DatasetView {
	
	Long id;
	String datasetIdentifier;
	String name;
	String description;
	
	Boolean retracted;
	Date dateCreated;
	
	Integer noGlycans;
	Integer noProteins = 0;
	
	License license;
	String version;
	Date versionDate;
	String versionComment;
	
	UserEntity user;
	
	List<Publication> publications;
	List<Grant> grants;
	List<DatabaseResourceDataset> integratedIn;
	List<DatabaseResource> associatedDatasources;
	List<Publication> associatedPapers;
	List<GlygenMetadataRow> data;
	List<DatasetVersion> versions;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
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
	public Integer getNoGlycans() {
		return noGlycans;
	}
	public void setNoGlycans(Integer noGlycans) {
		this.noGlycans = noGlycans;
	}
	public Integer getNoProteins() {
		return noProteins;
	}
	public void setNoProteins(Integer noProteins) {
		this.noProteins = noProteins;
	}
	public License getLicense() {
		return license;
	}
	public void setLicense(License license) {
		this.license = license;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public Date getVersionDate() {
		return versionDate;
	}
	public void setVersionDate(Date versionDate) {
		this.versionDate = versionDate;
	}
	public String getVersionComment() {
		return versionComment;
	}
	public void setVersionComment(String versionComment) {
		this.versionComment = versionComment;
	}
	public UserEntity getUser() {
		return user;
	}
	public void setUser(UserEntity user) {
		this.user = user;
	}
	public List<Publication> getPublications() {
		return publications;
	}
	public void setPublications(List<Publication> publications) {
		this.publications = publications;
	}
	public List<Grant> getGrants() {
		return grants;
	}
	public void setGrants(List<Grant> grants) {
		this.grants = grants;
	}
	public List<DatabaseResourceDataset> getIntegratedIn() {
		return integratedIn;
	}
	public void setIntegratedIn(List<DatabaseResourceDataset> integratedIn) {
		this.integratedIn = integratedIn;
	}
	public List<DatabaseResource> getAssociatedDatasources() {
		return associatedDatasources;
	}
	public void setAssociatedDatasources(List<DatabaseResource> associatedDatasources) {
		this.associatedDatasources = associatedDatasources;
	}
	public List<Publication> getAssociatedPapers() {
		return associatedPapers;
	}
	public void setAssociatedPapers(List<Publication> associatedPapers) {
		this.associatedPapers = associatedPapers;
	}
	public List<GlygenMetadataRow> getData() {
		return data;
	}
	public void setData(List<GlygenMetadataRow> data) {
		this.data = data;
	}
	public List<DatasetVersion> getVersions() {
		return versions;
	}
	public void setVersions(List<DatasetVersion> vers) {
		this.versions = vers;
	}
	
	
}
