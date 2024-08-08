package org.glygen.tablemaker.persistence.glycan;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Embeddable
public class DatatypeCategoryPK {
	Datatype datatype;
    DatatypeCategory category;

    @JsonIgnore
    @ManyToOne(targetEntity = Datatype.class)
    @JoinColumn(name = "datatypeid")
	public Datatype getDatatype() {
		return datatype;
	}

	public void setDatatype(Datatype datatype) {
		this.datatype = datatype;
	}

	@JsonIgnore
	@ManyToOne(targetEntity = DatatypeCategory.class)
    @JoinColumn(name = "categoryid")
	public DatatypeCategory getCategory() {
		return category;
	}

	public void setCategory(DatatypeCategory category) {
		this.category = category;
	}

}
