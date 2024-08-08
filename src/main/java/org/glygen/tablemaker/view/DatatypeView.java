package org.glygen.tablemaker.view;

import java.util.List;

import org.glygen.tablemaker.persistence.glycan.Datatype;
import org.glygen.tablemaker.persistence.glycan.Namespace;

public class DatatypeView {
	
	Long datatypeId;
    String uri;
    String name;
    String description;
    Namespace namespace;
    Boolean multiple = false;
    Boolean mandatory = false;
    List<String> allowedValues;
    
	public DatatypeView(Datatype d) {
		this.datatypeId = d.getDatatypeId();
		this.uri = d.getUri();
		this.name = d.getName();
		this.description = d.getDescription();
		this.namespace = d.getNamespace();
		this.multiple = d.getMultiple();
		this.allowedValues = d.getAllowedValues();
	}
	public Long getDatatypeId() {
		return datatypeId;
	}
	public void setDatatypeId(Long datatypeId) {
		this.datatypeId = datatypeId;
	}
	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
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
	public Namespace getNamespace() {
		return namespace;
	}
	public void setNamespace(Namespace namespace) {
		this.namespace = namespace;
	}
	public Boolean getMultiple() {
		return multiple;
	}
	public void setMultiple(Boolean multiple) {
		this.multiple = multiple;
	}
	public Boolean getMandatory() {
		return mandatory;
	}
	public void setMandatory(Boolean mandatory) {
		this.mandatory = mandatory;
	}
	public List<String> getAllowedValues() {
		return allowedValues;
	}
	public void setAllowedValues(List<String> allowedValues) {
		this.allowedValues = allowedValues;
	}

}
