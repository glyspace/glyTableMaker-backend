package org.glygen.tablemaker.exception;

import java.util.List;

import org.glygen.tablemaker.persistence.UploadErrorEntity;

@SuppressWarnings("serial")
public class BatchUploadException extends IllegalArgumentException {
	
	List<UploadErrorEntity> errors;
	
	public BatchUploadException() {
		super();
	}
	
	public BatchUploadException(String message) {
		super(message);
	}
	
	public BatchUploadException (String message, List<UploadErrorEntity> errors) {
		super (message);
		this.errors = errors;
	}
	
	public List<UploadErrorEntity> getErrors() {
		return errors;
	}
}
