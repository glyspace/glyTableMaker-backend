package org.glygen.tablemaker.persistence.glycan;

import java.util.Date;

import org.glygen.tablemaker.persistence.UserEntity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@Entity
@Table(name="composition_formats")
public class CompositionFormatDescriptor {

	@Id
    @GeneratedValue
	Long id;
	
	@Column(nullable=false)
	String name;
	
	@Column(length=4000)
	String description;
	
	@Column
	String example;
	
	@JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
	JsonNode definition;
	
	@Column
	Boolean system = false;
	
	@ManyToOne(targetEntity = UserEntity.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = true, name = "userid", foreignKey = @ForeignKey(name = "FK_VERIFY_USER"))
	UserEntity user;
	
	@Column(name="datecreated")
    @Temporal(TemporalType.DATE)
	Date dateCreated;
	
	@Column(name="dateModified")
    @Temporal(TemporalType.DATE)
	Date dateModified;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getExample() {
		return example;
	}

	public void setExample(String example) {
		this.example = example;
	}

	public JsonNode getDefinition() {
		return definition;
	}

	public void setDefinition(JsonNode definition) {
		this.definition = definition;
	}

	public Boolean getSystem() {
		return system;
	}

	public void setSystem(Boolean system) {
		this.system = system;
	}

	public UserEntity getUser() {
		return user;
	}

	public void setUser(UserEntity user) {
		this.user = user;
	}

	public Date getDateCreated() {
		return dateCreated;
	}

	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}

	public Date getDateModified() {
		return dateModified;
	}

	public void setDateModified(Date dateModified) {
		this.dateModified = dateModified;
	}
}
