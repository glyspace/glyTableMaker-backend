package org.glygen.tablemaker.persistence.table;

import java.util.List;

import org.glygen.tablemaker.persistence.glycan.Collection;
import org.glygen.tablemaker.view.ImageSettings;

public class TableView {
	
	List<Collection> collections;
	List<TableColumn> columns;
	FileFormat fileFormat;
	String filename;
	Double imageScale = 1.0;
	ImageSettings imageSettings;
	
	public List<Collection> getCollections() {
		return collections;
	}
	public void setCollections(List<Collection> collections) {
		this.collections = collections;
	}
	public List<TableColumn> getColumns() {
		return columns;
	}
	public void setColumns(List<TableColumn> colums) {
		this.columns = colums;
	}
	public FileFormat getFileFormat() {
		return fileFormat;
	}
	public void setFileFormat(FileFormat fileFormat) {
		this.fileFormat = fileFormat;
	}
	public String getFilename() {
		return filename;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
	public Double getImageScale() {
		return imageScale;
	}
	public void setImageScale(Double imageScale) {
		this.imageScale = imageScale;
	}
	public ImageSettings getImageSettings() {
		return imageSettings;
	}
	public void setImageSettings(ImageSettings imageSettings) {
		this.imageSettings = imageSettings;
	}
	

}
