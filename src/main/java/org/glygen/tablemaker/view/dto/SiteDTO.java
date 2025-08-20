package org.glygen.tablemaker.view.dto;

import org.glygen.tablemaker.persistence.protein.GlycoproteinSiteType;
import org.glygen.tablemaker.persistence.protein.SitePosition;

public class SiteDTO {
	String positionString;
	SitePosition position;
	GlycoproteinSiteType type;
	java.util.Collection<GlycanInSiteDTO> glycans;
	public String getPositionString() {
		return positionString;
	}
	public void setPositionString(String positionString) {
		this.positionString = positionString;
	}
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
	public java.util.Collection<GlycanInSiteDTO> getGlycans() {
		return glycans;
	}
	public void setGlycans(java.util.Collection<GlycanInSiteDTO> glycans) {
		this.glycans = glycans;
	}
}
