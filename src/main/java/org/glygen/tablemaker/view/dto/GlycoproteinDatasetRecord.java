package org.glygen.tablemaker.view.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

public class GlycoproteinDatasetRecord extends DatasetRecord {
	String uniProtId;
	String glytoucanId;
	String aminoAcid;
	String site;
	String residue;
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	String glycosylationType;
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	String glycosylationSubType;
	
	public String getUniProtId() {
		return uniProtId;
	}
	public void setUniProtId(String uniProtId) {
		this.uniProtId = uniProtId;
	}
	public String getGlytoucanId() {
		return glytoucanId;
	}
	public void setGlytoucanId(String glytoucanId) {
		this.glytoucanId = glytoucanId;
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
	public String getResidue() {
		return residue;
	}
	public void setResidue(String residue) {
		this.residue = residue;
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

}
