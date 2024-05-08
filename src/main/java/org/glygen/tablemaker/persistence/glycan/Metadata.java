package org.glygen.tablemaker.persistence.glycan;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlTransient;

@Entity
public class Metadata {
    Long metadataId;
    Datatype type;
    String value;
    String valueUri;
 	String valueId;
    Collection collection;
    
    /**
     * @return the id
     */
    @Id
    @GeneratedValue
    public Long getMetadataId() {
        return metadataId;
    }
    /**
     * @param datatypeId the id to set
     */
    public void setMetadataId(Long id) {
        this.metadataId = id;
    }
    /**
     * @return the type
     */
    @NotNull
    @ManyToOne(targetEntity = Datatype.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "datatypeid", foreignKey = @ForeignKey(name = "FK_VERIFY_DATATYPE"))
    public Datatype getType() {
        return type;
    }
    /**
     * @param type the type to set
     */
    public void setType(Datatype type) {
        this.type = type;
    }
    /**
     * @return the value
     */
    @NotEmpty
    @Column (name="value", columnDefinition="text", nullable=false)
    public String getValue() {
        return value;
    }
    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }
    
    /**
     * @return the collection
     */
    @ManyToOne(targetEntity = Collection.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "collectionid", foreignKey = @ForeignKey(name = "FK_VERIFY_COLLECTION"))
    @XmlTransient  // so that from the metadata we should not go back to collection - prevent cycles
	@JsonIgnore
    public Collection getCollection() {
        return collection;
    }
    
    public void setCollection(Collection collection) {
		this.collection = collection;
	}
    
    @Column(length=4000)
    public String getValueUri() {
		return valueUri;
	}
	public void setValueUri(String valueUri) {
		this.valueUri = valueUri;
	}
	
	@Column
	public String getValueId() {
		return valueId;
	}
	public void setValueId(String valueId) {
		this.valueId = valueId;
	}
}
