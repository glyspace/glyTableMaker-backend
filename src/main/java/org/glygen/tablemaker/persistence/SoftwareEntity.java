package org.glygen.tablemaker.persistence;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.xml.bind.annotation.XmlTransient;

@Entity
@Table(name="software")
public class SoftwareEntity {

	Long id;
	String softwareName;
	String pmid;
	String url;
	UserEntity user;
	
	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	
	@Column(name="name", nullable=false)
    @NotEmpty
	public String getSoftwareName() {
		return softwareName;
	}
	
	public void setSoftwareName(String softwareName) {
		this.softwareName = softwareName;
	}
	
	@Column
	public String getPmid() {
		return pmid;
	}
	public void setPmid(String pmid) {
		this.pmid = pmid;
	}
	
	@Column
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	
	@OneToOne
	@JsonIgnore
	public UserEntity getUser() {
		return user;
	}
	
	public void setUser(UserEntity user) {
		this.user = user;
	}
}
