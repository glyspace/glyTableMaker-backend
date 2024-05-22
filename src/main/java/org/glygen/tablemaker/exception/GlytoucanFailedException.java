package org.glygen.tablemaker.exception;

@SuppressWarnings("serial")
public class GlytoucanFailedException extends RuntimeException {
	
	String errorJson;
	
	public GlytoucanFailedException() {
        super();
    }

    public GlytoucanFailedException(String message, String errorJson) {
        super(message);
        this.errorJson = errorJson;
    }

    public GlytoucanFailedException(Throwable throwable) {
        super(throwable);
    }

    public GlytoucanFailedException(String message, String errorJson, Throwable throwable) {
        super(message, throwable);
        this.errorJson = errorJson;
    }

	public String getErrorJson() {
		return errorJson;
	}

	public void setErrorJson(String errorJson) {
		this.errorJson = errorJson;
	}

}
