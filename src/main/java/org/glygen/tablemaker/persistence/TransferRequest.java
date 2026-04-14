package org.glygen.tablemaker.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class TransferRequest {
	
	@Id
	@GeneratedValue
	Long id;
	
	@Column(nullable=false)
	String datasetHash;
	
	@Column(nullable=false)
	String datasetIdentifier;
	
	@Column
	Instant createdAt = Instant.now();
	
	public String getDatasetHash() {
		return datasetHash;
	}
	public void setDatasetHash(String datasetHash) {
		this.datasetHash = datasetHash;
	}
	public String getDatasetIdentifier() {
		return datasetIdentifier;
	}
	public void setDatasetIdentifier(String datasetIdentifier) {
		this.datasetIdentifier = datasetIdentifier;
	}
	public Instant getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
}
