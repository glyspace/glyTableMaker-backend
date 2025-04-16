package org.glygen.tablemaker.persistence;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;

@Entity
@Table (name="error_report")
public class ErrorReportEntity {
	
	String message;
	String details;
	Date dateReported;
	String ticketUrl;
	String ticketLabel;
	
	@Column (length=4000, nullable=false)
	@NotNull
	@Id
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	
	@Column (length=4000)
	public String getDetails() {
		return details;
	}
	public void setDetails(String details) {
		this.details = details;
	}
	
	@Column
	@Temporal(TemporalType.DATE)
	public Date getDateReported() {
		return dateReported;
	}
	public void setDateReported(Date dateReported) {
		this.dateReported = dateReported;
	}
	
	@Column (length=4000)
	public String getTicketUrl() {
		return ticketUrl;
	}
	public void setTicketUrl(String ticketUrl) {
		this.ticketUrl = ticketUrl;
	}
	
	@Column
	public String getTicketLabel() {
		return ticketLabel;
	}
	public void setTicketLabel(String ticketLabel) {
		this.ticketLabel = ticketLabel;
	}
}
