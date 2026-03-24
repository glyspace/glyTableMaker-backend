package org.glygen.tablemaker.view;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Software {

	String name;
	String url;
	String publication;
	
	@NotEmpty
	@Size(max=100, message="Name cannot exceed 100 characters")
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@Size(max=255, message="URL cannot exceed 255 characters")
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	
	@Size(max=50, message="Publication cannot exceed 50 characters")
	public String getPublication() {
		return publication;
	}
	public void setPublication(String publication) {
		this.publication = publication;
	}
}
