package org.glygen.tablemaker.view;

import java.util.List;

import org.glygen.tablemaker.persistence.dataset.DatabaseResource;
import org.glygen.tablemaker.persistence.dataset.DatabaseResourceDataset;
import org.glygen.tablemaker.persistence.dataset.Grant;
import org.glygen.tablemaker.persistence.dataset.License;
import org.glygen.tablemaker.persistence.dataset.Publication;

public class DatasetInputView {

	String name;
	String description;
	License license;
	List<Grant> grants;
	List<DatabaseResourceDataset> integratedIn;
	List<DatabaseResource> associatedDatasources;
	List<Publication> associatedPapers;
	List<CollectionView> collections;
	
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
	public License getLicense() {
		return license;
	}
	public void setLicense(License license) {
		this.license = license;
	}
	public List<Grant> getGrants() {
		return grants;
	}
	public void setGrants(List<Grant> grants) {
		this.grants = grants;
	}
	public List<DatabaseResourceDataset> getIntegratedIn() {
		return integratedIn;
	}
	public void setIntegratedIn(List<DatabaseResourceDataset> integratedIn) {
		this.integratedIn = integratedIn;
	}
	public List<DatabaseResource> getAssociatedDatasources() {
		return associatedDatasources;
	}
	public void setAssociatedDatasources(List<DatabaseResource> associatedDatasources) {
		this.associatedDatasources = associatedDatasources;
	}
	public List<Publication> getAssociatedPapers() {
		return associatedPapers;
	}
	public void setAssociatedPapers(List<Publication> associatedPapers) {
		this.associatedPapers = associatedPapers;
	}
	public List<CollectionView> getCollections() {
		return collections;
	}
	public void setCollections(List<CollectionView> collections) {
		this.collections = collections;
	}
}
