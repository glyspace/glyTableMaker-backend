package org.glygen.tablemaker.persistence;

import java.util.Date;

import org.glygen.tablemaker.persistence.glycan.UploadStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="uploads")
public class BatchUploadEntity {
	Long id;
	UploadStatus status;
	String error;
	Date startDate;
	Date accessedDate;
	String successMessage;
	
	@Id
	@GeneratedValue
	@Column(name="uploadid")
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	@Enumerated(EnumType.STRING)
	@Column(name="status", nullable=false)
	public UploadStatus getStatus() {
		return status;
	}
	public void setStatus(UploadStatus status) {
		this.status = status;
	}
	
	@Column(name="error", columnDefinition="text")
	public String getError() {
		return error;
	}
	public void setError(String error) {
		this.error = error;
	}
	
	@Column
	public Date getStartDate() {
		return startDate;
	}
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
	
	@Column
	public Date getAccessedDate() {
		return accessedDate;
	}
	public void setAccessedDate(Date accessedDate) {
		this.accessedDate = accessedDate;
	}
	
	@Column(name="message", length=2000)
	public String getSuccessMessage() {
		return successMessage;
	}
	public void setSuccessMessage(String sucessMessage) {
		this.successMessage = sucessMessage;
	}
}
