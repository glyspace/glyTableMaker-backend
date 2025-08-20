package org.glygen.tablemaker.view.dto;

public class GlycanInSiteDTO {
	String type;   // glycan, fragment, motif
    String glycosylationType;
    String glycosylationSubType;
    GlycanDTO glycan;
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
	public GlycanDTO getGlycan() {
		return glycan;
	}
	public void setGlycan(GlycanDTO glycan) {
		this.glycan = glycan;
	}
}
