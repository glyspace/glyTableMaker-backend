package org.glygen.tablemaker.persistence.protein;

import org.glygen.tablemaker.persistence.glycan.Glycan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;

@Entity
public class Site {
	
	Long siteId;
	String position;
	java.util.Collection<Glycan> glycans;
	
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
	
	@ManyToMany(fetch=FetchType.EAGER)
	@JoinTable(
		    name="site_glycans",
		    joinColumns=@JoinColumn(name="siteid"),
		    inverseJoinColumns=@JoinColumn(name="glycanid")
		)
	public java.util.Collection<Glycan> getGlycans() {
		return glycans;
	}
	
	public void setGlycans(java.util.Collection<Glycan> glycans) {
		this.glycans = glycans;
	}

}
