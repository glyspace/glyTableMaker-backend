package org.glygen.tablemaker.persistence.glycan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Datatype {
    Long datatypeId;
    String uri;
    String name;
    String description;
    String namespace;
    /**
     * @return the id
     */
    @Id
    @Column(name="datatypeid", unique = true, nullable = false)
    public Long getId() {
        return datatypeId;
    }
    /**
     * @param datatypeId the id to set
     */
    public void setId(Long id) {
        this.datatypeId = id;
    }
    /**
     * @return the uri
     */
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
     * @return the namespace
     */
    public String getNamespace() {
        return namespace;
    }
    /**
     * @param namespace the namespace to set
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

}
