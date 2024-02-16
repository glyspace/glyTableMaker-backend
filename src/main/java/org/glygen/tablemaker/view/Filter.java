package org.glygen.tablemaker.view;

public class Filter {
    String id;
    String value;
    
    public Filter() {
        // TODO Auto-generated constructor stub
    }
    
    public Filter (String id, String value) {
    	this.id = id;
    	this.value = value;
    }
    
    /**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}
    
    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }
    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }
    
}
