package org.glygen.tablemaker.view;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

@Schema(name = "Transfer", description = "Transfer Request")
public class Transfer {
	
	@Schema
	String userName;
	
	@Schema
	@NotEmpty
	String datasetIdentifier;
	
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getDatasetIdentifier() {
		return datasetIdentifier;
	}
	public void setDatasetIdentifier(String datasetIdentifier) {
		this.datasetIdentifier = datasetIdentifier;
	}
	
	

}
