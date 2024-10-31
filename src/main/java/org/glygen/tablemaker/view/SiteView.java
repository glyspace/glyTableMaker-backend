package org.glygen.tablemaker.view;

import org.glygen.tablemaker.persistence.protein.GlycoproteinSiteType;
import org.glygen.tablemaker.persistence.protein.SitePosition;

public class SiteView {
	SitePosition position;
	GlycoproteinSiteType type;
	java.util.Collection<GlycanInSiteView> glycans;
	
	public SitePosition getPosition() {
		return position;
	}
	public void setPosition(SitePosition position) {
		this.position = position;
	}
	public GlycoproteinSiteType getType() {
		return type;
	}
	public void setType(GlycoproteinSiteType type) {
		this.type = type;
	}
	public java.util.Collection<GlycanInSiteView> getGlycans() {
		return glycans;
	}
	public void setGlycans(java.util.Collection<GlycanInSiteView> glycans) {
		this.glycans = glycans;
	}

}
