package org.glygen.tablemaker.view;

import java.util.List;

import org.glygen.tablemaker.persistence.dataset.DatasetGlycoproteinMetadata;
import org.glygen.tablemaker.persistence.glycan.GlycanCartoon;

public class GlygenProteinMetadataRow {
	String rowId;
	List<DatasetGlycoproteinMetadata> columns;
	
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
}
