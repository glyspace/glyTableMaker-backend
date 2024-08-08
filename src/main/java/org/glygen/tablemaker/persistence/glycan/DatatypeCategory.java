package org.glygen.tablemaker.persistence.glycan;

import org.glygen.tablemaker.persistence.UserEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name="Category")
public class DatatypeCategory {
    
    Long categoryId;
    String name;
    String description;
    java.util.Collection<DatatypeInCategory> dataTypes;
    UserEntity user;
    
    /**
     * @return the id
     */
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="category_seq")
    @SequenceGenerator(name="category_seq", sequenceName="CATEGORY_SEQ", initialValue=10, allocationSize=50)
    @Column(name="categoryid", unique = true, nullable = false)
    public Long getCategoryId() {
        return categoryId;
    }
    /**
     * @param datatypeId the id to set
     */
    public void setCategoryId(Long id) {
        this.categoryId = id;
    }
    /**
     * @return the name
     */
    @Column(nullable=false)
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
     * @return the dataTypes
     */
    @OneToMany(mappedBy = "category", cascade=CascadeType.ALL, orphanRemoval = true)
    public java.util.Collection<DatatypeInCategory> getDataTypes() {
        return dataTypes;
    }
    /**
     * @param dataTypes the dataTypes to set
     */
    public void setDataTypes(java.util.Collection<DatatypeInCategory> dataTypes) {
        this.dataTypes = dataTypes;
    }
    
    /**
     * @return the user
     */
    @ManyToOne(targetEntity = UserEntity.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = true, name = "userid")
    public UserEntity getUser() {
        return user;
    }
    
    public void setUser(UserEntity user) {
		this.user = user;
	}

}
