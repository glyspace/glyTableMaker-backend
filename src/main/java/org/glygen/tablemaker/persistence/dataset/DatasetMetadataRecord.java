package org.glygen.tablemaker.persistence.dataset;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.xml.bind.annotation.XmlTransient;

public class DatasetMetadataRecord {
	@Id
    @GeneratedValue
	Long id;
	
	@Column
	String glytoucanId;
	
	@ManyToOne
	DatasetMetadataGroup metadataGroup;
	
	@ManyToOne(targetEntity = DatasetVersion.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "versionId", foreignKey = @ForeignKey(name = "FK_VERIFY_DATASET"))
    @XmlTransient  // so that from the metadata we should not go back to dataset - prevent cycles
	@JsonIgnore
	DatasetVersion dataset;

	public DatasetMetadataRecord(DatasetMetadataRecord dm) {
		this.glytoucanId = dm.glytoucanId;
		this.metadataGroup = dm.metadataGroup;
	}

	public DatasetMetadataRecord() {
		// TODO Auto-generated constructor stub
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getGlytoucanId() {
		return glytoucanId;
	}

	public void setGlytoucanId(String glytoucanId) {
		this.glytoucanId = glytoucanId;
	}

	public DatasetMetadataGroup getMetadataGroup() {
		return metadataGroup;
	}

	public void setMetadataGroup(DatasetMetadataGroup metadataGroup) {
		this.metadataGroup = metadataGroup;
	}

	public DatasetVersion getDataset() {
		return dataset;
	}

	public void setDataset(DatasetVersion dataset) {
		this.dataset = dataset;
	}
}
