package org.glygen.tablemaker.view;

import java.util.List;

import org.glygen.tablemaker.persistence.dataset.DatasetMetadata;

public class GlygenMetadataRow {
	
	String rowId;
	List<DatasetMetadata> columns;
	
	public String getRowId() {
		return rowId;
	}
	public void setRowId(String rowId) {
		this.rowId = rowId;
	}
	public List<DatasetMetadata> getColumns() {
		return columns;
	}
	public void setColumns(List<DatasetMetadata> columns) {
		this.columns = columns;
	}
	
	

}
