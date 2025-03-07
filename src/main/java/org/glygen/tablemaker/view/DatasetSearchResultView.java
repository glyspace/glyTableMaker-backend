package org.glygen.tablemaker.view;

import java.util.List;

public class DatasetSearchResultView {
	
	DatasetSearchInput input;
	List<DatasetView> objects;
	int totalItems;
	int filteredTotal;
	
	public DatasetSearchInput getInput() {
		return input;
	}
	public void setInput(DatasetSearchInput input) {
		this.input = input;
	}
	
	public int getFilteredTotal() {
		return filteredTotal;
	}
	public void setFilteredTotal(int filteredTotal) {
		this.filteredTotal = filteredTotal;
	}
	public List<DatasetView> getObjects() {
		return objects;
	}
	public void setObjects(List<DatasetView> objects) {
		this.objects = objects;
	}
	public int getTotalItems() {
		return totalItems;
	}
	public void setTotalItems(int totalItems) {
		this.totalItems = totalItems;
	}

}
