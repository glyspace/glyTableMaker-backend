package org.glygen.tablemaker.persistence.dataset;

import org.glygen.tablemaker.util.ResidueUtil;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.xml.bind.annotation.XmlTransient;

@Entity
public class DatasetGlycoproteinMetadataRecord {
	@Id
    @GeneratedValue
	Long id;
	
	@Column
	String uniProtId;
	
	@Column
	String glytoucanId;
	
	@Column
	String aminoAcid;
	
	@Column
	String site;
	
	@Column
	String residue;
	
	@Column
	String glycosylationType;
	
	@Column
	String glycosylationSubType;
	
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	DatasetMetadataGroup metadataGroup;
	
	@ManyToOne(targetEntity = DatasetVersion.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "versionId", foreignKey = @ForeignKey(name = "FK_VERIFY_DATASET"))
    @XmlTransient  // so that from the metadata we should not go back to dataset - prevent cycles
	@JsonIgnore
	DatasetVersion dataset;

	public DatasetGlycoproteinMetadataRecord(DatasetGlycoproteinMetadataRecord dm) {
		this.aminoAcid = dm.aminoAcid;
		this.glycosylationSubType = dm.glycosylationSubType;
		this.glycosylationType = dm.glycosylationType;
		this.glytoucanId = dm.glytoucanId;
		this.site = dm.site;
		this.uniProtId = dm.uniProtId;
		this.metadataGroup = dm.metadataGroup;
	}

	public DatasetGlycoproteinMetadataRecord() {
		// TODO Auto-generated constructor stub
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getGlytoucanId() {
		return glytoucanId;
	}

	public void setGlytoucanId(String glytoucanId) {
		this.glytoucanId = glytoucanId;
	}

	public DatasetMetadataGroup getMetadataGroup() {
		return metadataGroup;
	}

	public void setMetadataGroup(DatasetMetadataGroup metadataGroup) {
		this.metadataGroup = metadataGroup;
	}

	public DatasetVersion getDataset() {
		return dataset;
	}

	public void setDataset(DatasetVersion dataset) {
		this.dataset = dataset;
	}

	public String getUniProtId() {
		return uniProtId;
	}

	public void setUniProtId(String uniProtId) {
		this.uniProtId = uniProtId;
	}

	public String getAminoAcid() {
		return aminoAcid;
	}

	public void setAminoAcid(String aminoAcid) {
		this.aminoAcid = aminoAcid;
	}

	public String getSite() {
		return site;
	}

	public void setSite(String site) {
		this.site = site;
	}

	public String getGlycosylationType() {
		return glycosylationType;
	}

	public void setGlycosylationType(String glycosylationType) {
		this.glycosylationType = glycosylationType;
	}

	public String getGlycosylationSubType() {
		return glycosylationSubType;
	}

	public void setGlycosylationSubType(String glycosylationSubType) {
		this.glycosylationSubType = glycosylationSubType;
	}
	
	public String getResidue() {
		return residue;
	}
	
	public void setResidue(String residue) {
		this.residue = residue;
	}
	
	@PrePersist
    @PreUpdate
    private void computeResidue() {
        this.residue = ResidueUtil.buildResidue(this.aminoAcid, this.site, true);
    }
}
