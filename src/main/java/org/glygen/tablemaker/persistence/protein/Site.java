package org.glygen.tablemaker.persistence.protein;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;
import jakarta.xml.bind.annotation.XmlTransient;

@Entity
public class Site {
	
	Long siteId;
	String positionString;
	SitePosition position;
	GlycoproteinSiteType type;
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
	public String getPositionString() {
		return positionString;
	}
	public void setPositionString(String position) {
		this.positionString = position;
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
	
	@Column(name="type", length=50)
	@Enumerated(EnumType.STRING)
	public GlycoproteinSiteType getType() {
		return type;
	}
	public void setType(GlycoproteinSiteType type) {
		this.type = type;
	}
	
	@Transient
	public SitePosition getPosition() {
		return position;
	}
	public void setPosition(SitePosition position) {
		this.position = position;
	}

}
