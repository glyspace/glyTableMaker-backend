package org.glygen.tablemaker.view.dto;

import java.util.List;

import org.glygen.tablemaker.persistence.glycan.CollectionTag;
import org.glygen.tablemaker.persistence.glycan.CollectionType;
import org.glygen.tablemaker.persistence.glycan.Metadata;
import org.glygen.tablemaker.persistence.glycan.MetadataType;

import com.fasterxml.jackson.databind.JsonNode;

public class CollectionDTO {
    String name;
    String description;
    CollectionType type;
    List<GlycanDTO> glycans;
    List<GlycoproteinDTO> glycoproteins;
    JsonNode metadataValues;
    MetadataType sampleType;
    List<CollectionTag> tags;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public CollectionType getType() {
		return type;
	}
	public void setType(CollectionType type) {
		this.type = type;
	}
	public List<GlycanDTO> getGlycans() {
		return glycans;
	}
	public void setGlycans(List<GlycanDTO> glycans) {
		this.glycans = glycans;
	}
	public List<GlycoproteinDTO> getGlycoproteins() {
		return glycoproteins;
	}
	public void setGlycoproteins(List<GlycoproteinDTO> glycoproteins) {
		this.glycoproteins = glycoproteins;
	}
	public JsonNode getMetadataValues() {
		return metadataValues;
	}
	public void setMetadataValues(JsonNode metadataValues) {
		this.metadataValues = metadataValues;
	}
	public MetadataType getSampleType() {
		return sampleType;
	}
	public void setSampleType(MetadataType sampleType) {
		this.sampleType = sampleType;
	}
	public List<CollectionTag> getTags() {
		return tags;
	}
	public void setTags(List<CollectionTag> tags) {
		this.tags = tags;
	}

}
