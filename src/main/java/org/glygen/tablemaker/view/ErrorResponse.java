package org.glygen.tablemaker.view;
import java.time.Instant;

public class ErrorResponse {
    String code;
    String message;
    Instant timestamp;
    
    public ErrorResponse() {
        // TODO Auto-generated constructor stub
    }
    
    public ErrorResponse(String code, String message, Instant timestamp) {
        super();
        this.code = code;
        this.message = message;
        this.timestamp = timestamp;
    }

    /**
     * @return the code
     */
    public String getCode() {
        return code;
    }

    /**
     * @param code the code to set
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * @return the timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
    