package org.glygen.tablemaker.persistence;

import java.util.Date;

import org.glygen.tablemaker.persistence.protein.MultipleGlycanOrder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "upload_jobs")
public class BatchUploadJob {
	Long jobId;
	String tag;
	String fileType;
	BatchUploadEntity upload;
	MultipleGlycanOrder orderParam;
	Date lastRun;
	UserEntity user;
	
	@Id
	@GeneratedValue
	public Long getJobId() {
		return jobId;
	}
	public void setJobId(Long jobId) {
		this.jobId = jobId;
	}
	
	@Column
	public String getTag() {
		return tag;
	}
	public void setTag(String tag) {
		this.tag = tag;
	}
	
	@OneToOne
	@NotNull
	public BatchUploadEntity getUpload() {
		return upload;
	}
	public void setUpload(BatchUploadEntity upload) {
		this.upload = upload;
	}
	
	@Enumerated(EnumType.STRING)
	@Column(name="glycan_order")
	public MultipleGlycanOrder getOrderParam() {
		return orderParam;
	}
	public void setOrderParam(MultipleGlycanOrder orderParam) {
		this.orderParam = orderParam;
	}
	
	@Column
	public Date getLastRun() {
		return lastRun;
	}
	public void setLastRun(Date lastRun) {
		this.lastRun = lastRun;
	}
	
	/**
     * @return the user
     */
    @ManyToOne(targetEntity = UserEntity.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "userid", foreignKey = @ForeignKey(name = "FK_VERIFY_USER"))
	public UserEntity getUser() {
		return user;
	}
	public void setUser(UserEntity user) {
		this.user = user;
	}
	
	@Column
	public String getFileType() {
		return fileType;
	}
	public void setFileType(String fileType) {
		this.fileType = fileType;
	}
}
