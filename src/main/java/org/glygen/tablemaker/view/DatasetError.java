package org.glygen.tablemaker.view;

public class DatasetError {

	String message;
	Integer errorLevel = 1; // 0: warning, 1: error
	
	public DatasetError() {
	}
	
	public DatasetError (String message, int errorLevel) {
		this.message = message;
		this.errorLevel = errorLevel;
	}

	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}

	public Integer getErrorLevel() {
		return errorLevel;
	}
	public void setErrorLevel(Integer errorLevel) {
		this.errorLevel = errorLevel;
	}
	
	
}
