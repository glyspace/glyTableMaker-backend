package org.glygen.tablemaker.view;

import java.util.List;

import org.glygen.tablemaker.view.dto.DatasetGlycoproteinRowDTO;
import org.glygen.tablemaker.view.dto.DatasetRowDTO;

public class DatasetTableDownloadView {
	
	String filename;
	List<GlygenMetadataRow> data;
	List<GlygenProteinMetadataRow> glycoproteinData;
	
	List<DatasetRowDTO> records;
	List<DatasetGlycoproteinRowDTO> glycoproteinRecords;
	
	String version;
	
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
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public List<DatasetRowDTO> getRecords() {
		return records;
	}
	public void setRecords(List<DatasetRowDTO> records) {
		this.records = records;
	}
	public List<DatasetGlycoproteinRowDTO> getGlycoproteinRecords() {
		return glycoproteinRecords;
	}
	public void setGlycoproteinRecords(List<DatasetGlycoproteinRowDTO> glycoproteinRecords) {
		this.glycoproteinRecords = glycoproteinRecords;
	}
}
