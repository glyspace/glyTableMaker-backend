package org.glygen.tablemaker.view.dto;

import java.util.List;

import org.glygen.tablemaker.persistence.glycan.CollectionTag;
import org.glygen.tablemaker.persistence.glycan.CollectionType;
import org.glygen.tablemaker.persistence.glycan.Metadata;

public class CollectionDTO {
    String name;
    String description;
    CollectionType type;
    List<GlycanDTO> glycans;
    List<GlycoproteinDTO> glycoproteins;
    List<Metadata> metadata;
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
	public List<Metadata> getMetadata() {
		return metadata;
	}
	public void setMetadata(List<Metadata> metadata) {
		this.metadata = metadata;
	}
	public List<CollectionTag> getTags() {
		return tags;
	}
	public void setTags(List<CollectionTag> tags) {
		this.tags = tags;
	}

}
