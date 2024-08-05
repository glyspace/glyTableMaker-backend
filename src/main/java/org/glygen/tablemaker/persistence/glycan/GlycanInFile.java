package org.glygen.tablemaker.persistence.glycan;

import org.glygen.tablemaker.persistence.BatchUploadEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name="glycans_glycan_file_upload")
public class GlycanInFile {
	
	Long id;
	Glycan glycan;
	BatchUploadEntity uploadFile;
	Boolean isNew;
	
	/**
     * @return the id
     */
    @Id
    @GeneratedValue
    @Column(name="glycanuploadid")
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	/**
     * @return the glycan
     */
    @JsonIgnore
    @ManyToOne(targetEntity = Glycan.class)
    @JoinColumn(name = "glycanid")  
	public Glycan getGlycan() {
		return glycan;
	}
    
	public void setGlycan(Glycan glycan) {
		this.glycan = glycan;
	}
	
	/**
     * @return the batchuploadentity
     */
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "glycan_file_upload_id")
	public BatchUploadEntity getUploadFile() {
		return uploadFile;
	}
	public void setUploadFile(BatchUploadEntity uploadFile) {
		this.uploadFile = uploadFile;
	}
	
	@Column
	public Boolean getIsNew() {
		return isNew;
	}
	public void setIsNew(Boolean isNew) {
		this.isNew = isNew;
	}
}
