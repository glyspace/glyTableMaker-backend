package org.glygen.tablemaker.persistence.protein;

public class Position {
	
	Long location;
	String aminoAcid;
	
	public Long getLocation() {
		return location;
	}
	public void setLocation(Long location) {
		this.location = location;
	}
	public String getAminoAcid() {
		return aminoAcid;
	}
	public void setAminoAcid(String aminoAcid) {
		this.aminoAcid = aminoAcid;
	}

}
