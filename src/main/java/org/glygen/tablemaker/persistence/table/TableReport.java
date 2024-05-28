package org.glygen.tablemaker.persistence.table;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="reports")
public class TableReport {
	
	Long reportId;
	String reportJSON;
	
	@Id
	@GeneratedValue
	public Long getReportId() {
		return reportId;
	}
	public void setReportId(Long reportId) {
		this.reportId = reportId;
	}
	
	@Column(columnDefinition="text")
	public String getReportJSON() {
		return reportJSON;
	}
	public void setReportJSON(String reportJSON) {
		this.reportJSON = reportJSON;
	}
	
	

}
