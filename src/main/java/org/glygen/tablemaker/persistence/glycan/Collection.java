package org.glygen.tablemaker.persistence.glycan;

import org.glygen.tablemaker.persistence.UserEntity;

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

@Entity
@Table(name="collections")
public class Collection {
    
    Long id;
    String name;
    String description;
    UserEntity user;
    Metadata metadata;
    java.util.Collection<Collection> collections;   //TODO how to do this???
    java.util.Collection<GlycanInCollection> glycans;
    
    /**
     * @return the id
     */
    @Id
    @GeneratedValue
    @Column(name="collectionid")
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
     * @return the name
     */
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
     * @return the metadata
     */
    @ManyToOne(targetEntity = Metadata.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "metadataid", foreignKey = @ForeignKey(name = "FK_VERIFY_METADATA"))
    public Metadata getMetadata() {
        return metadata;
    }
    /**
     * @param metadata the metadata to set
     */
    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }
    /**
     * @return the collections
     */
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
    @OneToMany(mappedBy = "collection")
    public java.util.Collection<GlycanInCollection> getGlycans() {
        return glycans;
    }
    /**
     * @param glycans the glycans to set
     */
    public void setGlycans(java.util.Collection<GlycanInCollection> glycans) {
        this.glycans = glycans;
    }

}
