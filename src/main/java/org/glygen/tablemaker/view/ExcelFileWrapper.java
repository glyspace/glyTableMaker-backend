package org.glygen.tablemaker.view;

public class ExcelFileWrapper {
	
	Integer sheetNumber = 1;
	Integer columnNo = 1;
	Integer startRow = 1;
	
	public Integer getSheetNumber() {
		return sheetNumber;
	}
	public void setSheetNumber(Integer sheetNumber) {
		this.sheetNumber = sheetNumber;
	}
	public Integer getColumnNo() {
		return columnNo;
	}
	public void setColumnNo(Integer columnNo) {
		this.columnNo = columnNo;
	}
	public Integer getStartRow() {
		return startRow;
	}
	public void setStartRow(Integer rowNo) {
		this.startRow = rowNo;
	}
	
	

}
