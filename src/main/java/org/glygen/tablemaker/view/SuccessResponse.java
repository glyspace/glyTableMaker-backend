package org.glygen.tablemaker.view;

public class SuccessResponse<T> {
    
    T data;
    String message;
    
    public SuccessResponse() {
        // TODO Auto-generated constructor stub
    }
    
    public SuccessResponse (T d, String m) {
        this.data = d;
        this.message = m;
    }
    /**
     * @return the data
     */
    public T getData() {
        return data;
    }
    /**
     * @param data the data to set
     */
    public void setData(T data) {
        this.data = data;
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

}
