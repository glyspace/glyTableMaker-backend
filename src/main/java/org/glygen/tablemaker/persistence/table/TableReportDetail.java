package org.glygen.tablemaker.persistence.table;

import java.util.ArrayList;
import java.util.List;

public class TableReportDetail {
	Boolean success;
	List<String> errors;
	List<String> warnings;
	String message;
	
	public Boolean getSuccess() {
		return success;
	}
	public void setSuccess(Boolean success) {
		this.success = success;
	}
	public List<String> getErrors() {
		return errors;
	}
	public void setErrors(List<String> errors) {
		this.errors = errors;
	}
	public List<String> getWarnings() {
		return warnings;
	}
	public void setWarnings(List<String> warnings) {
		this.warnings = warnings;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	
	public void addError (String error) {
		if (errors == null) {
			errors = new ArrayList<>();
		}
		if (!errors.contains(error))
			errors.add(error);
	}
	
	public void addWarning (String warning) {
		if (warnings == null) {
			warnings = new ArrayList<>();
		}
		if (!warnings.contains(warning))
			warnings.add(warning);
	}
}
