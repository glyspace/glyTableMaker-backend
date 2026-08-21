package org.glygen.tablemaker.view.dto;

import com.fasterxml.jackson.databind.JsonNode;

public class DatasetGlycoproteinRowDTO {
	private Long id;
    private String glytoucanId;
    private String uniProtId;
    private String aminoAcid;
    private String site;
    private String glycosylationType;
    private String glycosylationSubType;
    private JsonNode metadata;
    private String version;
    
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
	public JsonNode getMetadata() {
		return metadata;
	}
	public void setMetadata(JsonNode metadata) {
		this.metadata = metadata;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
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
}
