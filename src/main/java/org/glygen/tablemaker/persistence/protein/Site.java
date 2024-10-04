package org.glygen.tablemaker.persistence.protein;

import org.glygen.tablemaker.persistence.glycan.Collection;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.xml.bind.annotation.XmlTransient;

@Entity
public class Site {
	
	Long siteId;
	String position;
	java.util.Collection<GlycanInSite> glycans;
	Glycoprotein glycoprotein;
	
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
	
	@ManyToOne(targetEntity = Glycoprotein.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "glycoproteinid", foreignKey = @ForeignKey(name = "FK_VERIFY_GLYCOPROTEIN"))
    @XmlTransient  // - prevent cycles
	@JsonIgnore
	public Glycoprotein getGlycoprotein() {
		return glycoprotein;
	}
	public void setGlycoprotein(Glycoprotein glycoprotein) {
		this.glycoprotein = glycoprotein;
	}

}
