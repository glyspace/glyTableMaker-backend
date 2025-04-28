package org.glygen.tablemaker.view;

import java.util.ArrayList;
import java.util.List;

import org.glygen.tablemaker.persistence.glycan.GlycanTag;
import org.glygen.tablemaker.persistence.protein.GlycanInSite;
import org.glygen.tablemaker.persistence.protein.Glycoprotein;
import org.glygen.tablemaker.persistence.protein.Site;

public class GlycoproteinView {
	
	Long id;
	String uniprotId;
	String name;
	String proteinName;
	String sequence;
	String sequenceVersion;
	String geneSymbol;
	
	List<SiteView> sites;
	List<GlycanTag> tags;
	
	public GlycoproteinView() {
	}

	public GlycoproteinView(Glycoprotein p) {
		this.geneSymbol = p.getGeneSymbol();
		this.name = p.getName();
		this.proteinName = p.getProteinName();
		this.sequence = p.getSequence();
		this.sequenceVersion = p.getSequenceVersion();
		this.uniprotId = p.getUniprotId();
		this.id = p.getId();
		if (p.getSites() != null) {
			this.sites = new ArrayList<>();
			for (Site s: p.getSites()) {
				SiteView sv = new SiteView();
				sv.setSiteId(s.getSiteId());
				sv.setPosition(s.getPosition());
				sv.setType(s.getType());
				sv.setGlycans(new ArrayList<>());
				for (GlycanInSite gc: s.getGlycans()) {
					GlycanInSiteView gis = new GlycanInSiteView();
					if (gc.getGlycan() == null) {  // no glycans, only glycosylation types
						sv.setGlycosylationType(gc.getGlycosylationType());
						sv.setGlycosylationSubType(gc.getGlycosylationSubType());
					} else {
						gis.setGlycan(gc.getGlycan());
						gis.setGlycosylationSubType(gc.getGlycosylationSubType());
						gis.setGlycosylationType(gc.getGlycosylationType());
						gis.setType(gc.getType());
						sv.getGlycans().add(gis);
					}
				}
				this.sites.add(sv);
			}
		}
		if (p.getTags() != null) this.tags = new ArrayList<>(p.getTags());
	}

	public String getUniprotId() {
		return uniprotId;
	}
	
	public void setUniprotId(String uniprotId) {
		this.uniprotId = uniprotId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSequence() {
		return sequence;
	}

	public void setSequence(String sequence) {
		this.sequence = sequence;
	}

	public String getGeneSymbol() {
		return geneSymbol;
	}

	public void setGeneSymbol(String geneSymbol) {
		this.geneSymbol = geneSymbol;
	}

	public List<SiteView> getSites() {
		return sites;
	}

	public void setSites(List<SiteView> sites) {
		this.sites = sites;
	}
	
	public List<GlycanTag> getTags() {
		return tags;
	}
	
	public void setTags(List<GlycanTag> tags) {
		this.tags = tags;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getProteinName() {
		return proteinName;
	}

	public void setProteinName(String proteinName) {
		this.proteinName = proteinName;
	}

	public String getSequenceVersion() {
		return sequenceVersion;
	}

	public void setSequenceVersion(String sequenceVersion) {
		this.sequenceVersion = sequenceVersion;
	}
}
