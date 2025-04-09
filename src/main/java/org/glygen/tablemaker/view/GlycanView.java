package org.glygen.tablemaker.view;

import org.glygen.tablemaker.persistence.glycan.CompositionType;

public class GlycanView {
    String sequence;
    SequenceFormat format;
    String glytoucanID;
    String composition;
    CompositionType type;
    
    /**
     * @return the sequence
     */
    public String getSequence() {
        return sequence;
    }
    /**
     * @param sequence the sequence to set
     */
    public void setSequence(String sequence) {
        this.sequence = sequence;
    }
    /**
     * @return the format
     */
    public SequenceFormat getFormat() {
        return format;
    }
    /**
     * @param format the format to set
     */
    public void setFormat(SequenceFormat format) {
        this.format = format;
    }
    /**
     * @return the glytoucanID
     */
    public String getGlytoucanID() {
        return glytoucanID;
    }
    /**
     * @param glytoucanID the glytoucanID to set
     */
    public void setGlytoucanID(String glytoucanID) {
        this.glytoucanID = glytoucanID;
    }
    /**
     * @return the composition
     */
    public String getComposition() {
        return composition;
    }
    /**
     * @param composition the composition to set
     */
    public void setComposition(String composition) {
        this.composition = composition;
    }
	public CompositionType getType() {
		return type;
	}
	public void setType(CompositionType type) {
		this.type = type;
	}
}
