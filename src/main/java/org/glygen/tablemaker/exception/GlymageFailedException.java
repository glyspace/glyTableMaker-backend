package org.glygen.tablemaker.exception;

import org.json.JSONObject;

@SuppressWarnings("serial")
public class GlymageFailedException extends Exception {
	
	JSONObject response;
	
	public GlymageFailedException() {
		super();
	}
	
	public GlymageFailedException(String message, JSONObject errorJson) {
        super(message);
        this.response = errorJson;
    }

    public GlymageFailedException(Throwable throwable) {
        super(throwable);
    }

    public GlymageFailedException(String message, JSONObject errorJson, Throwable throwable) {
        super(message, throwable);
        this.response = errorJson;
    }

	public void setResponse(JSONObject response) {
		this.response = response;
	}
	
	public JSONObject getResponse() {
		return response;
	}

}
