package org.glygen.tablemaker.view.dto;

import java.util.Date;

import com.fasterxml.jackson.databind.JsonNode;

public class CompositionFormatView {
	Long id;
	String name;
	String description;
	String example;
	Boolean system;
	Date dateCreated;
	Date dateModified;
	JsonNode definition;

	public CompositionFormatView() {
		// TODO Auto-generated constructor stub
	}

	public CompositionFormatView(Long id, String name, String description, String example, Boolean system, Date dateCreated,
			Date dateModified) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.example = example;
		this.system = system;
		this.dateCreated = dateCreated;
		this.dateModified = dateModified;
	}
	
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
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

	public Boolean getSystem() {
		return system;
	}

	public void setSystem(Boolean system) {
		this.system = system;
	}

	public Date getDateCreated() {
		return dateCreated;
	}

	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}

	public Date getDateModified() {
		return dateModified;
	}

	public void setDateModified(Date dateModified) {
		this.dateModified = dateModified;
	}
	
	public String getExample() {
		return example;
	}
	
	public void setExample(String example) {
		this.example = example;
	}

	public void setDefinition(JsonNode definition) {
		this.definition = definition;
	}
	
	public JsonNode getDefinition() {
		return definition;
	}
}
