package org.glygen.tablemaker.view;

public class TagView {
    private Long tagId;
    private String label;
    private Long glycanCount;
    private Long glycoproteinCount;

    public TagView() {}
    public TagView(Long tagId, String label, Long glycanCount, Long glycoproteinCount) {
        this.tagId = tagId; this.label = label;
        this.glycanCount = glycanCount; this.glycoproteinCount = glycoproteinCount;
    }
	public Long getTagId() {
		return tagId;
	}
	public void setTagId(Long tagId) {
		this.tagId = tagId;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public Long getGlycanCount() {
		return glycanCount;
	}
	public void setGlycanCount(Long glycanCount) {
		this.glycanCount = glycanCount;
	}
	public Long getGlycoproteinCount() {
		return glycoproteinCount;
	}
	public void setGlycoproteinCount(Long glycoproteinCount) {
		this.glycoproteinCount = glycoproteinCount;
	}
    
}