package org.glygen.tablemaker.persistence.protein;

import org.glygen.tablemaker.persistence.glycan.Glycan;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name="site_glycans")
public class GlycanInSite {
	
	Long id;
    Glycan glycan;
    Site site;
    
    String type;

    @Id
    @GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}


	@JsonIgnore
    @ManyToOne(targetEntity = Glycan.class)
    @JoinColumn(name = "glycanid")  
	public Glycan getGlycan() {
		return glycan;
	}

	public void setGlycan(Glycan glycan) {
		this.glycan = glycan;
	}

	@JsonIgnore
    @ManyToOne(targetEntity = Site.class)
    @JoinColumn(name = "siteid")
	public Site getSite() {
		return site;
	}

	public void setSite(Site site) {
		this.site = site;
	}

	@Column
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

}
