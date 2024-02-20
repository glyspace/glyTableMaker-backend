package org.glygen.tablemaker.persistence.glycan;

import java.util.List;

import org.glygen.tablemaker.persistence.UserEntity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name="collections")
public class Collection {
    Long collectionId;
    String name;
    String description;
    UserEntity user;
    java.util.Collection<Metadata> metadata;
    Collection parent;
    java.util.Collection<Collection> collections;  
    java.util.Collection<GlycanInCollection> glycans;
    
    /**
     * @return the id
     */
    @Id
    @GeneratedValue
    @Column(name="collectionid")
    public Long getCollectionId() {
        return collectionId;
    }
    /**
     * @param id the id to set
     */
    public void setCollectionId(Long id) {
        this.collectionId = id;
    }
    /**
     * @return the name
     */
    @Column
    public String getName() {
        return name;
    }
    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
    /**
     * @return the description
     */
    @Column(length=4000)
    public String getDescription() {
        return description;
    }
    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }
    /**
     * @return the user
     */
    @ManyToOne(targetEntity = UserEntity.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "userid", foreignKey = @ForeignKey(name = "FK_VERIFY_USER"))
    public UserEntity getUser() {
        return user;
    }
    /**
     * @param user the user to set
     */
    public void setUser(UserEntity user) {
        this.user = user;
    }
    
    /**
     * @return the collections
     */
    @OneToMany(mappedBy="parent")
    public java.util.Collection<Collection> getCollections() {
        return collections;
    }
    /**
     * @param collections the collections to set
     */
    
    public void setCollections(java.util.Collection<Collection> collections) {
        this.collections = collections;
    }
    /**
     * @return the glycans
     */
    @JsonManagedReference
    @OneToMany(mappedBy = "collection", cascade=CascadeType.ALL)
    public java.util.Collection<GlycanInCollection> getGlycans() {
        return glycans;
    }
    /**
     * @param glycans the glycans to set
     */
    public void setGlycans(java.util.Collection<GlycanInCollection> glycans) {
        this.glycans = glycans;
    }
    /**
     * @return the parent
     */
    @ManyToOne
    public Collection getParent() {
        return parent;
    }
    /**
     * @param parent the parent to set
     */
    public void setParent(Collection parent) {
        this.parent = parent;
    }
    
    @Transient
	public java.util.Collection<Metadata> getMetadata() {
		return metadata;
	}
	public void setMetadata(List<Metadata> metadata) {
		this.metadata = metadata;
	}

}
