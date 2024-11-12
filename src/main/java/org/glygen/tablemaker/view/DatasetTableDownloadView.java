package org.glygen.tablemaker.view;

import java.util.List;

public class DatasetTableDownloadView {
	
	String filename;
	List<GlygenMetadataRow> data;
	List<GlygenProteinMetadataRow> glycoproteinData;
	
	public String getFilename() {
		return filename;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
	public List<GlygenMetadataRow> getData() {
		return data;
	}
	public void setData(List<GlygenMetadataRow> data) {
		this.data = data;
	}
	public List<GlygenProteinMetadataRow> getGlycoproteinData() {
		return glycoproteinData;
	}
	public void setGlycoproteinData(List<GlygenProteinMetadataRow> glycoproteinData) {
		this.glycoproteinData = glycoproteinData;
	}
}
