package org.glygen.tablemaker.view;

import org.glygen.tablemaker.persistence.glycan.Glycan;

public class GlycanInSiteView {
	Glycan glycan;
    String type;   // glycan, fragment, motif
    String glycosylationType;
    String glycosylationSubType;
    
	public Glycan getGlycan() {
		return glycan;
	}
	public void setGlycan(Glycan glycan) {
		this.glycan = glycan;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
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
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof GlycanInSiteView) {
			if (((GlycanInSiteView) obj).getGlycan() != null) {
				if (this.glycan.getGlycanId() != null && ((GlycanInSiteView) obj).getGlycan().getGlycanId() != null) {
					return this.glycan.getGlycanId().equals (((GlycanInSiteView) obj).getGlycan().getGlycanId());
				} else if (this.glycan.getGlytoucanID() != null && ((GlycanInSiteView) obj).getGlycan().getGlytoucanID() != null) {
					return this.glycan.getGlytoucanID().equals (((GlycanInSiteView) obj).getGlycan().getGlytoucanID());
				} else if (this.glycan.getWurcs() != null && ((GlycanInSiteView) obj).getGlycan().getWurcs() != null) {
					return this.glycan.getWurcs().equals (((GlycanInSiteView) obj).getGlycan().getWurcs());
				} else if (this.glycan.getGlycoCT() != null && ((GlycanInSiteView) obj).getGlycan().getGlycoCT() != null) {
					return this.glycan.getGlycoCT().equals(((GlycanInSiteView) obj).getGlycan().getGlycoCT());
				}
			}
		}
		return super.equals(obj);
	}
	
	@Override
	public int hashCode() {
		if (this.glycan != null) {
			if (this.glycan.getGlycanId() != null) {
				return this.glycan.getGlycanId().hashCode();
			} else if (this.glycan.getGlytoucanID() != null) {
				return this.glycan.getGlytoucanID().hashCode();
			} else if (this.glycan.getWurcs() != null) {
				return this.glycan.getWurcs().hashCode();
			} else if (this.glycan.getGlycoCT() != null) {
				return this.glycan.getGlycoCT().hashCode();
			}
		}
		return super.hashCode();
	}

}
