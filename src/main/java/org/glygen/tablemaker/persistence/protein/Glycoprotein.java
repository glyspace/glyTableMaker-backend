package org.glygen.tablemaker.persistence.protein;

import java.util.ArrayList;
import java.util.Date;

import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.glycan.GlycanTag;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@Entity
@Table(name="glycoproteins")
public class Glycoprotein {
	
	Long id;
	String uniprotId;
	String name;
	String proteinName;
	String description;
	String geneSymbol;
	String sequence;
	
	Date dateCreated;
    UserEntity user;
    
    java.util.Collection<Site> sites;
    java.util.Collection<GlycanTag> tags;
    java.util.Collection<GlycoproteinInFile> uploadFiles;
    java.util.Collection<GlycoproteinInCollection> glycoproteinCollections;
    
    @Id
	@GeneratedValue
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	
	@Column(name="uniprotid", unique = false, nullable = false)
	public String getUniprotId() {
		return uniprotId;
	}
	public void setUniprotId(String uniprotId) {
		this.uniprotId = uniprotId;
	}
	
	@Column
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@Column(length=4000)
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	@OneToMany(mappedBy = "glycoprotein", cascade=CascadeType.ALL, orphanRemoval = true)
	public java.util.Collection<Site> getSites() {
		return sites;
	}
	public void setSites(java.util.Collection<Site> sites) {
		this.sites = sites;
	}
	
	@Column(name="datecreated")
    @Temporal(TemporalType.DATE)
	public Date getDateCreated() {
		return dateCreated;
	}
	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}
	
	@ManyToOne(targetEntity = UserEntity.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "userid", foreignKey = @ForeignKey(name = "FK_VERIFY_USER"))
	public UserEntity getUser() {
		return user;
	}
	public void setUser(UserEntity user) {
		this.user = user;
	}
	
	@ManyToMany(fetch = FetchType.EAGER)
	public java.util.Collection<GlycanTag> getTags() {
		return tags;
	}
	public void setTags(java.util.Collection<GlycanTag> tags) {
		this.tags = tags;
	}
	
	public void addTag(GlycanTag tag) {
		if (tags == null) 
			tags = new ArrayList<>();
		tags.add(tag);
	}
	
	public boolean hasTag(String tag) {
		if (this.getTags() != null) {
			for (GlycanTag t: tags) {
				if (t.getLabel().equalsIgnoreCase(tag)) {
					return true;
				}
			}
		}
		return false;
	}
	
	@OneToMany(mappedBy = "glycoprotein", fetch=FetchType.EAGER, cascade=CascadeType.ALL, orphanRemoval=true)	
	public java.util.Collection<GlycoproteinInFile> getUploadFiles() {
		return uploadFiles;
	}
	public void setUploadFiles(java.util.Collection<GlycoproteinInFile> uploadFiles) {
		this.uploadFiles = uploadFiles;
	}
	
	@OneToMany(mappedBy = "glycoprotein")
	public java.util.Collection<GlycoproteinInCollection> getGlycoproteinCollections() {
		return glycoproteinCollections;
	}
	public void setGlycoproteinCollections(java.util.Collection<GlycoproteinInCollection> collections) {
		this.glycoproteinCollections = collections;
	}
	
	@Column
	public String getGeneSymbol() {
		return geneSymbol;
	}
	public void setGeneSymbol(String geneSymbol) {
		this.geneSymbol = geneSymbol;
	}
	
	@Column(columnDefinition="text")
	public String getSequence() {
		return sequence;
	}
	public void setSequence(String sequence) {
		this.sequence = sequence;
	}
	
	@Column
	public String getProteinName() {
		return proteinName;
	}
	public void setProteinName(String proteinName) {
		this.proteinName = proteinName;
	}
}
