package org.glygen.tablemaker.view;

public class StatisticsView {
	
	Long userCount=0L;
    Long datasetCount=0L;
    Long glycanCount=0L;
    Long proteinCount=0L;
    Long newGlycanCount=0L;
    
	public Long getUserCount() {
		return userCount;
	}
	public void setUserCount(Long userCount) {
		this.userCount = userCount;
	}
	public Long getDatasetCount() {
		return datasetCount;
	}
	public void setDatasetCount(Long datasetCount) {
		this.datasetCount = datasetCount;
	}
	public Long getGlycanCount() {
		return glycanCount;
	}
	public void setGlycanCount(Long glycanCount) {
		this.glycanCount = glycanCount;
	}
	public Long getProteinCount() {
		return proteinCount;
	}
	public void setProteinCount(Long proteinCount) {
		this.proteinCount = proteinCount;
	}
	public Long getNewGlycanCount() {
		return newGlycanCount;
	}
	public void setNewGlycanCount(Long newGlycanCount) {
		this.newGlycanCount = newGlycanCount;
	}

}
