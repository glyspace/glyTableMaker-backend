package org.glygen.tablemaker.persistence.protein;

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
@Table(name="glycoproteins_file_upload")
public class GlycoproteinInFile {
	
	Long id;
	Glycoprotein glycoprotein;
	BatchUploadEntity uploadFile;
	Boolean isNew;     // used to keep track of glycans newly added from this file
	
	/**
     * @return the id
     */
    @Id
    @GeneratedValue
    @Column(name="glyccoproteinnuploadid")
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
    @ManyToOne(targetEntity = Glycoprotein.class)
    @JoinColumn(name = "glycoproteinid")  
	public Glycoprotein getGlycoprotein() {
		return glycoprotein;
	}
    
	public void setGlycoprotein(Glycoprotein glycoprotein) {
		this.glycoprotein = glycoprotein;
	}
	
	/**
     * @return the batchuploadentity
     */
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "file_upload_id")
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
