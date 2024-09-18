package org.glygen.tablemaker.view;

import java.util.List;

import org.glygen.tablemaker.persistence.glycan.CollectionTag;
import org.glygen.tablemaker.persistence.glycan.Glycan;
import org.glygen.tablemaker.persistence.glycan.GlycanTag;
import org.glygen.tablemaker.persistence.glycan.Metadata;

public class CollectionView {
	Long collectionId;
	String name;
	String description;
	List<Metadata> metadata;
	List<Glycan> glycans;
	List<CollectionView> children;
	List<CollectionTag> tags;
	
	List<DatasetError> errors;
	List<DatasetError> warnings;
	
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
	public List<Metadata> getMetadata() {
		return metadata;
	}
	public void setMetadata(List<Metadata> metadata) {
		this.metadata = metadata;
	}
	public List<Glycan> getGlycans() {
		return glycans;
	}
	public void setGlycans(List<Glycan> glycans) {
		this.glycans = glycans;
	}
	public Long getCollectionId() {
		return collectionId;
	}
	public void setCollectionId(Long collectionId) {
		this.collectionId = collectionId;
	}
	public List<CollectionView> getChildren() {
		return children;
	}
	public void setChildren(List<CollectionView> children) {
		this.children = children;
	}
	public List<CollectionTag> getTags() {
		return tags;
	}
	public void setTags(List<CollectionTag> tags) {
		this.tags = tags;
	}
	public List<DatasetError> getErrors() {
		return errors;
	}
	public void setErrors(List<DatasetError> errors) {
		this.errors = errors;
	}
	public List<DatasetError> getWarnings() {
		return warnings;
	}
	public void setWarnings(List<DatasetError> warnings) {
		this.warnings = warnings;
	}
}
