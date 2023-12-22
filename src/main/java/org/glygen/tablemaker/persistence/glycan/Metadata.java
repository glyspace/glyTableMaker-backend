package org.glygen.tablemaker.persistence.glycan;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Metadata {
    Long id;
    Datatype type;
    String value;
    
    /**
     * @return the id
     */
    @Id
    public Long getId() {
        return id;
    }
    /**
     * @param datatypeId the id to set
     */
    public void setId(Long id) {
        this.id = id;
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
    public String getValue() {
        return value;
    }
    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }
}
