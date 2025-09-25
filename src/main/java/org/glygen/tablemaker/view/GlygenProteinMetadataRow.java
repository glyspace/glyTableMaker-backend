package org.glygen.tablemaker.view;

import java.util.List;

import org.glygen.tablemaker.persistence.dataset.DatasetGlycoproteinMetadata;

public class GlygenProteinMetadataRow {
	String rowId;
	List<DatasetGlycoproteinMetadata> columns;
	byte[] cartoon;
	
	public GlygenProteinMetadataRow() {
	}
	
	public GlygenProteinMetadataRow(String rowId, List<DatasetGlycoproteinMetadata> columns) {
		this.rowId = rowId;
		this.columns = columns;
	}
	
	public String getRowId() {
		return rowId;
	}
	public void setRowId(String rowId) {
		this.rowId = rowId;
	}
	public List<DatasetGlycoproteinMetadata> getColumns() {
		return columns;
	}
	public void setColumns(List<DatasetGlycoproteinMetadata> columns) {
		this.columns = columns;
	}
	
	public byte[] getCartoon() {
		return cartoon;
	}
	
	public void setCartoon(byte[] cartoon) {
		this.cartoon = cartoon;
	}
}
