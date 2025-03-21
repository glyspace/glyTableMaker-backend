package org.glygen.tablemaker.exception;

public class GlytoucanAPIFailedException extends Exception {

	public GlytoucanAPIFailedException() {
		super();
	}
	
	public GlytoucanAPIFailedException(String message) {
		super(message);
	}
	
	public GlytoucanAPIFailedException(Throwable throwable) {
        super(throwable);
    }
}
