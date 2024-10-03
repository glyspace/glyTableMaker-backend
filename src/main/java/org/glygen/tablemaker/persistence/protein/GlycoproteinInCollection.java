package org.glygen.tablemaker.persistence.protein;

import java.util.Date;

import org.glygen.tablemaker.persistence.glycan.Collection;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
@Table(name="glycoprotein_collection")
public class GlycoproteinInCollection {
	Long id;
    Glycoprotein glycoprotein;
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
     * @return the glycoprotein
     */
    @JsonIgnore
    @ManyToOne(targetEntity = Glycoprotein.class)
    @JoinColumn(name = "glycoproteinid")  
    public Glycoprotein getGlycoprotein() {
        return glycoprotein;
    }
    
    public void setGlycoprotein(Glycoprotein glycoprotein) {
		this.glycoprotein = glycoprotein;
	}
    
    /**
     * @return the collection
     */
    @JsonIgnore
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
