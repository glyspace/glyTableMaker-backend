package org.glygen.tablemaker.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="application_settings")
public class ApplicationSettingsEntity {
	
	String name;
	String value;
	
	@Id
	@Column
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@Column
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}

}
