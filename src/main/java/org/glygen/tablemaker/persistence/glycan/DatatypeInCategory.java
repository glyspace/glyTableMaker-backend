package org.glygen.tablemaker.persistence.glycan;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name="datatype_category")
public class DatatypeInCategory {
	DatatypeCategoryPK id;
	Datatype datatype;
	DatatypeCategory category;
	Boolean mandatory = false;
	
	@EmbeddedId
	public DatatypeCategoryPK getId() {
		return id;
	}
	public void setId(DatatypeCategoryPK id) {
		this.id = id;
	}
    
	@JsonIgnore
    @ManyToOne(targetEntity = Datatype.class)
    @JoinColumn(name = "datatypeid", insertable=false, updatable=false)  
	public Datatype getDatatype() {
		return datatype;
	}
	public void setDatatype(Datatype datatype) {
		this.datatype = datatype;
	}
	
	@JsonIgnore
    @ManyToOne(targetEntity = DatatypeCategory.class)
    @JoinColumn(name = "categoryid", insertable=false, updatable=false)
	public DatatypeCategory getCategory() {
		return category;
	}
	public void setCategory(DatatypeCategory category) {
		this.category = category;
	}
	
	@Column
	public Boolean getMandatory() {
		return mandatory;
	}
	public void setMandatory(Boolean mandatory) {
		this.mandatory = mandatory;
	}
}
