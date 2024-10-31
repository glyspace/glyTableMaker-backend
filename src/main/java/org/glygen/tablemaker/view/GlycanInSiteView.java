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

}
