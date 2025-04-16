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
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Position)
			return ((Position) obj).getLocation().equals (this.location);
		return super.equals(obj);
	}
	
	@Override
	public int hashCode() {
		if (location != null) return location.hashCode();
		return super.hashCode();
	}

}
