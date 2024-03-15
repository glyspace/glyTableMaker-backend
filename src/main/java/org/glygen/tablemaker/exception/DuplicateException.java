package org.glygen.tablemaker.exception;

/**
 * trigger for duplicate exception
 */
public class DuplicateException extends RuntimeException {
	
	Object duplicate;
	
    public DuplicateException() {
        super();
    }

    public DuplicateException(String message) {
        super(message);
    }

    public DuplicateException(String message, Throwable throwable) {
        super(message, throwable);
    }
    
    public DuplicateException(String message, Throwable throwable, Object duplicate) {
        super(message, throwable);
        this.duplicate = duplicate;
    }
    
    public Object getDuplicate() {
		return duplicate;
	}
}
