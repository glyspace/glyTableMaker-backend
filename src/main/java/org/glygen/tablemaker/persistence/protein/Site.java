package org.glygen.tablemaker.persistence.protein;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class Site {
	
	Long siteId;
	String position;
	java.util.Collection<GlycanInSite> glycans;
	
	@Id
	@GeneratedValue
	public Long getSiteId() {
		return siteId;
	}
	public void setSiteId(Long siteId) {
		this.siteId = siteId;
	}
	
	@Column
	public String getPosition() {
		return position;
	}
	public void setPosition(String position) {
		this.position = position;
	}
	
	@OneToMany(mappedBy = "site")
	public java.util.Collection<GlycanInSite> getGlycans() {
		return glycans;
	}
	
	public void setGlycans(java.util.Collection<GlycanInSite> glycans) {
		this.glycans = glycans;
	}

}
