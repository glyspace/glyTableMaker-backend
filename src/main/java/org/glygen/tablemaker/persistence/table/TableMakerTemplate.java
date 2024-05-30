package org.glygen.tablemaker.persistence.table;

import java.util.Collection;

import org.glygen.tablemaker.persistence.UserEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.UniqueConstraint;

@Entity
public class TableMakerTemplate {
	
	Long templateId;
	String name;
	String description;
	Collection<TableColumn> columns;
	UserEntity user;
	
	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="tabletemplate_seq")
    @SequenceGenerator(name="tabletemplate_seq", sequenceName="TABLETEMPLATE_SEQ", initialValue=50, allocationSize = 50)
	public Long getTemplateId() {
		return templateId;
	}
	public void setTemplateId(Long templateId) {
		this.templateId = templateId;
	}
	
	@Column(unique=true, nullable=false)
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@Column(length=4000)
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	@ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinTable(name = "tablemakertemplate_table_column", joinColumns = { 
            @JoinColumn(name = "templateid", nullable = false) }, 
            inverseJoinColumns = { @JoinColumn(name = "columnid", 
                    nullable = false) }, uniqueConstraints={
            @UniqueConstraint(columnNames = {"columnid", "templateid"})})
	public Collection<TableColumn> getColumns() {
		return columns;
	}
	public void setColumns(Collection<TableColumn> columns) {
		this.columns = columns;
	}
	
	@ManyToOne(targetEntity = UserEntity.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = true, name = "userid", foreignKey = @ForeignKey(name = "FK_VERIFY_USER"))
	public UserEntity getUser() {
		return user;
	}
	public void setUser(UserEntity user) {
		this.user = user;
	}

}
