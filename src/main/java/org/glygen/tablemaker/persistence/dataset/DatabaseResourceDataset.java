package org.glygen.tablemaker.persistence.dataset;

import java.util.Date;


import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@Entity
@Table(name="dataset_databaseresource")
public class DatabaseResourceDataset {
	
	@Id
	@GeneratedValue
	Long databaseResourceId;
	
    @ManyToOne(targetEntity = DatabaseResource.class)
    @JoinColumn(name = "resourceid")  
	DatabaseResource resource;
	
	@Column(name="datepublished")
    @Temporal(TemporalType.DATE)
	Date date;
	
	@Column
	String version;    // version of the dataset that was integrated in
	
	@Column
	String versionInResource;   // version of the resource this data set was integrated in
	
	@JsonIgnore
    @ManyToOne(targetEntity=Dataset.class)
    @JoinColumn(name = "datasetId")
	Dataset dataset;

	public DatabaseResource getResource() {
		return resource;
	}

	public void setResource(DatabaseResource database) {
		this.resource = database;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Dataset getDataset() {
		return dataset;
	}

	public void setDataset(Dataset dataset) {
		this.dataset = dataset;
	}

	public Long getDatabaseResourceId() {
		return databaseResourceId;
	}

	public void setDatabaseResourceId(Long databaseResourceId) {
		this.databaseResourceId = databaseResourceId;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getVersionInResource() {
		return versionInResource;
	}

	public void setVersionInResource(String versionInResource) {
		this.versionInResource = versionInResource;
	}
}
