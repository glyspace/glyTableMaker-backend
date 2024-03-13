package org.glygen.tablemaker.persistence.glycan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Metadata {
    Long metadataId;
    Datatype type;
    String value;
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
    public Collection getCollection() {
        return collection;
    }
    
    public void setCollection(Collection collection) {
		this.collection = collection;
	}
}
