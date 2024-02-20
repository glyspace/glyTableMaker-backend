package org.glygen.tablemaker.persistence.glycan;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@Entity
@Table(name="glycan_collection")
public class GlycanInCollection {
    Long id;
    Glycan glycan;
    Collection collection;
    Date dateAdded;
    
    /**
     * @return the id
     */
    @Id
    @GeneratedValue
    @Column(name="glycancollectionid")
    public Long getId() {
        return id;
    }
    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }
    /**
     * @return the glycan
     */
    @JsonBackReference
    @ManyToOne(targetEntity = Glycan.class)
    @JoinColumn(name = "glycanid")  
    public Glycan getGlycan() {
        return glycan;
    }
    /**
     * @param glycan the glycan to set
     */
    public void setGlycan(Glycan glycan) {
        this.glycan = glycan;
    }
    /**
     * @return the collection
     */
    @JsonBackReference
    @ManyToOne(targetEntity = Collection.class)
    @JoinColumn(name = "collectionid")
    public Collection getCollection() {
        return collection;
    }
    /**
     * @param collection the collection to set
     */
    public void setCollection(Collection collection) {
        this.collection = collection;
    }
    /**
     * @return the dateAdded
     */
    @Column(name = "dateadded")
    @Temporal(TemporalType.DATE)
    public Date getDateAdded() {
        return dateAdded;
    }
    /**
     * @param dateAdded the dateAdded to set
     */
    public void setDateAdded(Date dateAdded) {
        this.dateAdded = dateAdded;
    }

}
