package org.glygen.tablemaker.persistence.dataset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class DatabaseResource {
	Long id;
	String name;   // name of the database/resource
	String identifer;   // identifier in the database/resource
	String URL;    // URL of the database/resource
	
	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	
	@Column
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@Column
	public String getURL() {
		return URL;
	}
	public void setURL(String uRL) {
		URL = uRL;
	}
	
	@Column
	public String getIdentifer() {
		return identifer;
	}
	public void setIdentifer(String identifer) {
		this.identifer = identifer;
	}
}
