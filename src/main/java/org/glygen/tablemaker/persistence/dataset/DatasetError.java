package org.glygen.tablemaker.persistence.dataset;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.xml.bind.annotation.XmlTransient;

@Entity
public class DatasetError {

	Long datasetErrorId;
	String message;
	Integer errorLevel = 1; // 0: warning, 1: error
	DatasetVersion dataset;
	
	public DatasetError() {
		// TODO Auto-generated constructor stub
	}
	
	public DatasetError (String message, int errorLevel) {
		this.message = message;
		this.errorLevel = errorLevel;
	}

	@Id
	@GeneratedValue
	public Long getDatasetErrorId() {
		return datasetErrorId;
	}
	public void setDatasetErrorId(Long datasetErrorId) {
		this.datasetErrorId = datasetErrorId;
	}
	
	@Column
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	
	@Column
	public Integer getErrorLevel() {
		return errorLevel;
	}
	public void setErrorLevel(Integer errorLevel) {
		this.errorLevel = errorLevel;
	}
	
	@ManyToOne(targetEntity = DatasetVersion.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "versionId", foreignKey = @ForeignKey(name = "FK_VERIFY_DATASET"))
    @XmlTransient  // so that from the errors we should not go back to datasetversion - prevent cycles
	@JsonIgnore
	public DatasetVersion getDataset() {
		return dataset;
	}
	public void setDataset(DatasetVersion dataset) {
		this.dataset = dataset;
	}
	
}
