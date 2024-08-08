package org.glygen.tablemaker.view;

import java.util.ArrayList;
import java.util.List;

import org.glygen.tablemaker.persistence.glycan.DatatypeCategory;

public class DatatypeCategoryView {
	
	Long categoryId;
    String name;
    String description;
    List<DatatypeView> dataTypes;
    
    /**
     * constructor to copy basic fields, does not add the datatypes
     * 
     * @param c DatatypeCategory to copy from
     */
	public DatatypeCategoryView(DatatypeCategory c) {
		this.categoryId = c.getCategoryId();
		this.name = c.getName();
		this.description = c.getDescription();
		this.dataTypes = new ArrayList<>();
	}
	public Long getCategoryId() {
		return categoryId;
	}
	public void setCategoryId(Long categoryId) {
		this.categoryId = categoryId;
	}
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
	public List<DatatypeView> getDataTypes() {
		return dataTypes;
	}
	public void setDataTypes(List<DatatypeView> dataTypes) {
		this.dataTypes = dataTypes;
	}

}
