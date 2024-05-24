package org.glygen.tablemaker.persistence.table;

import org.glygen.tablemaker.persistence.glycan.Datatype;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.xml.bind.annotation.XmlTransient;

@Entity
@Table(name="table_column")
public class TableColumn {
	Long columnId;
	Integer order;
	String name;
	Datatype datatype;
	ValueType type;
	GlycanColumns glycanColumn;
	String defaultValue;
	
	@Id
	@GeneratedValue
	public Long getColumnId() {
		return columnId;
	}
	public void setColumnId(Long columnId) {
		this.columnId = columnId;
	}
	
	@Column(name="column_order")
	public Integer getOrder() {
		return order;
	}
	public void setOrder(Integer order) {
		this.order = order;
	}
	
	@Column
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@ManyToOne(targetEntity = Datatype.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = true, name = "datatypeid", foreignKey = @ForeignKey(name = "FK_VERIFY_DATATYPE"))
    public Datatype getDatatype() {
        return datatype;
    }
	
	public void setDatatype(Datatype datatype) {
		this.datatype = datatype;
	}
	
	@Enumerated(EnumType.STRING)
	@Column(name="valuetype")
	public ValueType getType() {
		return type;
	}
	public void setType(ValueType type) {
		this.type = type;
	}
	
	@Enumerated(EnumType.STRING)
	@Column(name="glycancolumn")
	public GlycanColumns getGlycanColumn() {
		return glycanColumn;
	}
	public void setGlycanColumn(GlycanColumns glycanColumn) {
		this.glycanColumn = glycanColumn;
	}
	
	@Column(name="defaultvalue", columnDefinition="text")
	public String getDefaultValue() {
		return defaultValue;
	}
	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}
}
