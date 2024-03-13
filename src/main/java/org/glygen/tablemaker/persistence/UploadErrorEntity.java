package org.glygen.tablemaker.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name="glycan_file_errors")
public class UploadErrorEntity {
	Long id;
	String position; // row id etc.
	String message;
	String sequence;
	BatchUploadEntity upload;
	
	public UploadErrorEntity() {
		// TODO Auto-generated constructor stub
	}
	
	public UploadErrorEntity (String position, String message, String sequence) {
		this.position=position;
		this.message=message;
		this.sequence=sequence;
	}
	
	@Id
	@GeneratedValue
	@Column(name="errorid")
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	
	@Column
	public String getPosition() {
		return position;
	}
	public void setPosition(String position) {
		this.position = position;
	}
	
	@Column (length=4000, nullable=false)
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	
	@Column(name="sequence", columnDefinition="text")
	public String getSequence() {
		return sequence;
	}
	public void setSequence(String sequence) {
		this.sequence = sequence;
	}

	@ManyToOne(targetEntity = BatchUploadEntity.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "glycan_file_upload_id")
	public BatchUploadEntity getUpload() {
		return upload;
	}

	public void setUpload(BatchUploadEntity upload) {
		this.upload = upload;
	}

}
