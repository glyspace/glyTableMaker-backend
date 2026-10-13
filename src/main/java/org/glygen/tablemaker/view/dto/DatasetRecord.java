package org.glygen.tablemaker.view.dto;

import org.glygen.tablemaker.persistence.glycan.MetadataType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;

public abstract class DatasetRecord {
	
	@JdbcTypeCode(SqlTypes.JSON)
	JsonNode value;
	
	MetadataType sampleType;

	public JsonNode getValue() {
		return value;
	}

	public void setValue(JsonNode value) {
		this.value = value;
	}

	public MetadataType getSampleType() {
		return sampleType;
	}

	public void setSampleType(MetadataType sampleType) {
		this.sampleType = sampleType;
	}
	
	

}
