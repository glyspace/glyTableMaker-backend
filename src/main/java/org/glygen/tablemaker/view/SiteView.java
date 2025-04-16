package org.glygen.tablemaker.view;

import org.glygen.tablemaker.persistence.protein.GlycoproteinSiteType;
import org.glygen.tablemaker.persistence.protein.SitePosition;

public class SiteView {
	Long siteId;
	SitePosition position;
	GlycoproteinSiteType type;
	java.util.Collection<GlycanInSiteView> glycans;
	String glycosylationType;
	String glycosylationSubType;
	
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
	public String getGlycosylationType() {
		return glycosylationType;
	}
	public void setGlycosylationType(String glycosylationType) {
		this.glycosylationType = glycosylationType;
	}
	public String getGlycosylationSubType() {
		return glycosylationSubType;
	}
	public void setGlycosylationSubType(String glycosylationSubType) {
		this.glycosylationSubType = glycosylationSubType;
	}
	public Long getSiteId() {
		return siteId;
	}
	public void setSiteId(Long siteId) {
		this.siteId = siteId;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SiteView) {
			if (((SiteView) obj).getGlycans() != null && this.getGlycans() != null &&
				((SiteView) obj).getGlycans().size() == this.getGlycans().size()) {
				boolean allFound = true;
				for (GlycanInSiteView gis: glycans) {
					if (!((SiteView) obj).getGlycans().contains(gis)) {
						allFound = false;
					}
				}	
				if (allFound) {
					// check the positions
					if (this.position != null && this.position.equals(((SiteView) obj).getPosition()) &&
							this.type != null && this.type == ((SiteView) obj).type) {
						return true;
					}
				}
			}
		}
		return super.equals(obj);
	}
}
