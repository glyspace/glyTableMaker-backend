package org.glygen.tablemaker.persistence.glycan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name="Namespace")
public class Namespace {
	
	Long namespaceId;
	String name;
	String dictionary;
	Boolean hasUri = false;
	Boolean hasId = false;
	String fileIdentifier;
	
	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="namespace_seq")
    @SequenceGenerator(name="namespace_seq", sequenceName="NAMESPACE_SEQ", initialValue=50, allocationSize = 50)
    @Column(name="namespaceid", unique = true, nullable = false)
	public Long getNamespaceId() {
		return namespaceId;
	}
	public void setNamespaceId(Long namespaceId) {
		this.namespaceId = namespaceId;
	}
	
	@Column(nullable=false)
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@Column(length=4000)
	public String getDictionary() {
		return dictionary;
	}
	public void setDictionary(String dictionary) {
		this.dictionary = dictionary;
	}
	
	@Column
	public Boolean getHasUri() {
		return hasUri;
	}
	public void setHasUri(Boolean hasUri) {
		this.hasUri = hasUri;
	}
	
	@Column
	public Boolean getHasId() {
		return hasId;
	}
	public void setHasId(Boolean hasId) {
		this.hasId = hasId;
	}
	
	@Column
	public String getFileIdentifier() {
		return fileIdentifier;
	}
	public void setFileIdentifier(String fileIdentifier) {
		this.fileIdentifier = fileIdentifier;
	}
	
	

}
