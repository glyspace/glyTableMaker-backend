package org.glygen.tablemaker.view.dto;

import java.util.Date;
import java.util.List;

import org.glygen.tablemaker.persistence.dataset.License;
import org.glygen.tablemaker.persistence.dataset.Publication;
import org.glygen.tablemaker.persistence.glycan.CollectionType;

import com.fasterxml.jackson.annotation.JsonFormat;

public class DatasetVersionDTO {
	
	String version;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
	Date versionDate;
	String comment;
	License license;
	CollectionType type;
	
	List<DatasetRecord> records;
	List<Publication> publications;

	public List<DatasetRecord> getRecords() {
		return records;
	}

	public void setRecords(List<DatasetRecord> records) {
		this.records = records;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public Date getVersionDate() {
		return versionDate;
	}

	public void setVersionDate(Date versionDate) {
		this.versionDate = versionDate;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public License getLicense() {
		return license;
	}

	public void setLicense(License license) {
		this.license = license;
	}

	public CollectionType getType() {
		return type;
	}

	public void setType(CollectionType type) {
		this.type = type;
	}

	public List<Publication> getPublications() {
		return publications;
	}

	public void setPublications(List<Publication> publications) {
		this.publications = publications;
	}

}
