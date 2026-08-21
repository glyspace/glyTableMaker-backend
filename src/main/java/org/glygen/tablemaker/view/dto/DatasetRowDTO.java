package org.glygen.tablemaker.view.dto;

import com.fasterxml.jackson.databind.JsonNode;

public class DatasetRowDTO {
	private Long id;
    private String glytoucanId;
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
}
