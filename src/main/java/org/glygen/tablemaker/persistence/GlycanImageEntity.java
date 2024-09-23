package org.glygen.tablemaker.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table (name="glycanimages")
public class GlycanImageEntity {
	
	Long glycanId;
	String glytoucanId;
	String wurcs;
	
	@Id
	@Column(unique=true, nullable=false)
	public Long getGlycanId() {
		return glycanId;
	}
	public void setGlycanId(Long glycanId) {
		this.glycanId = glycanId;
	}
	
	@Column(length = 8, unique=true)
	public String getGlytoucanId() {
		return glytoucanId;
	}
	public void setGlytoucanId(String glytoucanId) {
		this.glytoucanId = glytoucanId;
	}
	
	@Column(columnDefinition="text")
	public String getWurcs() {
		return wurcs;
	}
	public void setWurcs(String wurcs) {
		this.wurcs = wurcs;
	}

}
