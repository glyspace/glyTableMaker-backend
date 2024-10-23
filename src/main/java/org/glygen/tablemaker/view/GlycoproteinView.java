package org.glygen.tablemaker.view;

import java.util.ArrayList;
import java.util.List;

import org.glygen.tablemaker.persistence.glycan.GlycanTag;
import org.glygen.tablemaker.persistence.protein.Glycoprotein;
import org.glygen.tablemaker.persistence.protein.Site;

public class GlycoproteinView {
	
	String uniprotId;
	String name;
	String sequence;
	String geneSymbol;
	
	List<Site> sites;
	List<GlycanTag> tags;
	
	public GlycoproteinView() {
		// TODO Auto-generated constructor stub
	}

	public GlycoproteinView(Glycoprotein p) {
		this.geneSymbol = p.getGeneSymbol();
		this.name = p.getName();
		this.sequence = p.getSequence();
		this.uniprotId = p.getUniprotId();
		if (p.getSites() != null) this.sites = new ArrayList<>(p.getSites());
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

	public List<Site> getSites() {
		return sites;
	}

	public void setSites(List<Site> sites) {
		this.sites = sites;
	}
	
	public List<GlycanTag> getTags() {
		return tags;
	}
	
	public void setTags(List<GlycanTag> tags) {
		this.tags = tags;
	}
}
