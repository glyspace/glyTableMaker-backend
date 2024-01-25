package org.glygen.tablemaker.view;

public class Filter {
    Integer column;
    String value;
    
    public Filter() {
        // TODO Auto-generated constructor stub
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
    /**
     * @return the column
     */
    public Integer getColumn() {
        return column;
    }
    /**
     * @param column the column to set
     */
    public void setColumn(Integer column) {
        this.column = column;
    }
}
