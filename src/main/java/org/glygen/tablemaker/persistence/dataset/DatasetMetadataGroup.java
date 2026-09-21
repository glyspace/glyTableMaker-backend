package org.glygen.tablemaker.persistence.dataset;

import org.glygen.tablemaker.persistence.glycan.MetadataType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class DatasetMetadataGroup {
	@Id
    @GeneratedValue
	Long id;
	
	@JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
	JsonNode value;
	
	@Enumerated(EnumType.STRING)
	MetadataType sampleType;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
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
