package org.glygen.tablemaker.persistence.dataset;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

@Entity
public class Retraction {
	
	@Id
	Long retractionId;
	
	@Column
	Date retractionDate;
	
	@Column
	String reason;
	
	@OneToOne
	Dataset dataset;

	public Date getRetractionDate() {
		return retractionDate;
	}

	public void setRetractionDate(Date retractionDate) {
		this.retractionDate = retractionDate;
	}

	public String getReason() {
		return reason;
	}
	
	public void setReason(String reason) {
		this.reason = reason;
	}

	public Dataset getDataset() {
		return dataset;
	}

	public void setDataset(Dataset dataset) {
		this.dataset = dataset;
	}

	public Long getRetractionId() {
		return retractionId;
	}

	public void setRetractionId(Long retractionId) {
		this.retractionId = retractionId;
	}
}
