package org.glygen.tablemaker.persistence.dataset;

import java.util.Collection;
import java.util.Date;

import org.glygen.tablemaker.persistence.UserEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotEmpty;

@Table
@Entity
public class Dataset {
	
	Long datasetId;
	String datasetIdentifier;
	String name;
	String description;
	Boolean retracted;
	Date dateCreated;
	
	UserEntity user;
	
	Collection<Publication> publications;
	Collection<Grant> grants;
	Collection<DatabaseResourceDataset> integratedIn;
	Collection<DatabaseResource> associatedDatasources;
	Collection<Publication> associatedPapers;
	Collection<DatasetVersion> versions;
	
	/**
     * @return the id
     */
    @Id
    @GeneratedValue
	public Long getDatasetId() {
		return datasetId;
	}
	public void setDatasetId(Long datasetId) {
		this.datasetId = datasetId;
	}
	
	@NotEmpty
	@Column (nullable=false)
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	
    @Column (name="description", columnDefinition="text")
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	@ManyToMany
	@JoinTable(
		    name="dataset_publications",
		    joinColumns=@JoinColumn(name="datasetId"),
		    inverseJoinColumns=@JoinColumn(name="id")
		)
	public Collection<Publication> getPublications() {
		return publications;
	}
	public void setPublications(Collection<Publication> publication) {
		this.publications = publication;
	}
	
	@ManyToMany(fetch=FetchType.EAGER)
	@JoinTable(
	    name="dataset_grants",
	    joinColumns=@JoinColumn(name="datasetId"),
	    inverseJoinColumns=@JoinColumn(name="id")
	)
	public Collection<Grant> getGrants() {
		return grants;
	}
	public void setGrants(Collection<Grant> grants) {
		this.grants = grants;
	}
	
	@ManyToOne(targetEntity = UserEntity.class, fetch = FetchType.EAGER)
	@JoinColumn(nullable = false, name = "userid", foreignKey = @ForeignKey(name = "FK_VERIFY_USER"))
	public UserEntity getUser() {
		return user;
	}
	public void setUser(UserEntity user) {
		this.user = user;
	}
	
	@Column(name="datecreated")
    @Temporal(TemporalType.DATE)
	public Date getDateCreated() {
		return dateCreated;
	}
	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}
	
	@OneToMany(mappedBy = "dataset", fetch=FetchType.EAGER, cascade=CascadeType.ALL, orphanRemoval=true)
	public Collection<DatabaseResourceDataset> getIntegratedIn() {
		return integratedIn;
	}
	public void setIntegratedIn(Collection<DatabaseResourceDataset> integratedIn) {
		this.integratedIn = integratedIn;
	}
	
	@ManyToMany(fetch=FetchType.EAGER)
	@JoinTable(
	    name="dataset_databases",
	    joinColumns=@JoinColumn(name="datasetId"),
	    inverseJoinColumns=@JoinColumn(name="id")
	)
	public Collection<DatabaseResource> getAssociatedDatasources() {
		return associatedDatasources;
	}
	public void setAssociatedDatasources(Collection<DatabaseResource> associatedDatabases) {
		this.associatedDatasources = associatedDatabases;
	}
	
	@ManyToMany(fetch=FetchType.EAGER)
	@JoinTable(
	    name="dataset_associated_papers",
	    joinColumns=@JoinColumn(name="datasetId"),
	    inverseJoinColumns=@JoinColumn(name="id")
	)
	public Collection<Publication> getAssociatedPapers() {
		return associatedPapers;
	}
	public void setAssociatedPapers(Collection<Publication> associatedPapers) {
		this.associatedPapers = associatedPapers;
	}
	
	@OneToMany(mappedBy = "dataset", cascade=CascadeType.ALL, orphanRemoval = true)
	public Collection<DatasetVersion> getVersions() {
		return versions;
	}
	
	public void setVersions(Collection<DatasetVersion> version) {
		this.versions = version;
	}
	
	@Column (nullable=false)
	public String getDatasetIdentifier() {
		return datasetIdentifier;
	}
	public void setDatasetIdentifier(String datasetIdentifier) {
		this.datasetIdentifier = datasetIdentifier;
	}
	
	@Column
	public Boolean getRetracted() {
		return retracted;
	}
	public void setRetracted(Boolean retracted) {
		this.retracted = retracted;
	}
}
