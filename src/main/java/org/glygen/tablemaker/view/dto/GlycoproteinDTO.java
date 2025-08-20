package org.glygen.tablemaker.view.dto;

import java.util.Date;

import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.glycan.GlycanTag;

public class GlycoproteinDTO {
	String uniprotId;
	String name;
	String proteinName;
	String description;
	String geneSymbol;
	String sequence;
	String sequenceVersion;
	Date dateCreated;
    UserEntity user;
    Date dateAdded;  // date added to the collection
    java.util.Collection<SiteDTO> sites;
    java.util.Collection<GlycanTag> tags;
	public String getUniprotId() {
		return uniprotId;
	}
	public void setUniprotId(String uniprotId) {
		this.uniprotId = uniprotId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getProteinName() {
		return proteinName;
	}
	public void setProteinName(String proteinName) {
		this.proteinName = proteinName;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getGeneSymbol() {
		return geneSymbol;
	}
	public void setGeneSymbol(String geneSymbol) {
		this.geneSymbol = geneSymbol;
	}
	public String getSequence() {
		return sequence;
	}
	public void setSequence(String sequence) {
		this.sequence = sequence;
	}
	public String getSequenceVersion() {
		return sequenceVersion;
	}
	public void setSequenceVersion(String sequenceVersion) {
		this.sequenceVersion = sequenceVersion;
	}
	public Date getDateCreated() {
		return dateCreated;
	}
	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}
	public UserEntity getUser() {
		return user;
	}
	public void setUser(UserEntity user) {
		this.user = user;
	}
	public Date getDateAdded() {
		return dateAdded;
	}
	public void setDateAdded(Date dateAdded) {
		this.dateAdded = dateAdded;
	}
	public java.util.Collection<SiteDTO> getSites() {
		return sites;
	}
	public void setSites(java.util.Collection<SiteDTO> sites) {
		this.sites = sites;
	}
	public java.util.Collection<GlycanTag> getTags() {
		return tags;
	}
	public void setTags(java.util.Collection<GlycanTag> tags) {
		this.tags = tags;
	}
}
