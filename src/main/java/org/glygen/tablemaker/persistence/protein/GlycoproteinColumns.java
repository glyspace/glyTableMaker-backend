package org.glygen.tablemaker.persistence.protein;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum GlycoproteinColumns {
	
	GLYTOUCANID ("GlyTouCanID"),
	UNIPROTID ("UniProtID"),
	AMINOACID ("AminoAcid"),
	SITE("Site"),
	GLYCOSYLATIONTYPE ("GlycosylationType"),
	GLYCOSYLATIONSUBTYPE ("GlycosylationSubtype");
	
	String label;
	
	@JsonCreator
	public static GlycoproteinColumns forValue(String value) {
		if (value.toLowerCase().equals("glytoucanid"))
			return GLYTOUCANID;
		else if (value.toLowerCase().equals("uniprotid"))
			return UNIPROTID;
		else if (value.toLowerCase().equals("aminoacid"))
            return AMINOACID;
		else if (value.toLowerCase().equals("site"))
            return SITE;
		else if (value.toLowerCase().equals("glycosylationtype"))
            return GLYCOSYLATIONTYPE;
		else if (value.toLowerCase().equals("glycosylationsubtype"))
            return GLYCOSYLATIONSUBTYPE;
		return GLYTOUCANID;
	}
	
	private GlycoproteinColumns(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
	
	
	public void setLabel(String label) {
		this.label = label;
	}
	
	@JsonValue
    public String external() { return label; }

}
