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

@Entity
public class Datatype {
    Long datatypeId;
    String uri;
    String name;
    String description;
    String namespace;
    UserEntity user;
    /**
     * @return the id
     */
    @Id
    @GeneratedValue
    @Column(name="datatypeid", unique = true, nullable = false)
    public Long getDatatypeId() {
        return datatypeId;
    }
    /**
     * @param datatypeId the id to set
     */
    public void setDatatypeId(Long id) {
        this.datatypeId = id;
    }
    /**
     * @return the uri
     */
    @Column
    public String getUri() {
        return uri;
    }
    /**
     * @param uri the uri to set
     */
    public void setUri(String uri) {
        this.uri = uri;
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
    @Column
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
     * @return the namespace
     */
    @Column
    public String getNamespace() {
        return namespace;
    }
    /**
     * @param namespace the namespace to set
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
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

}
