package org.glygen.tablemaker.persistence.glycan;

import org.glygen.tablemaker.persistence.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;

@Entity
public class Datatype {
    Long datatypeId;
    String uri;
    String name;
    String description;
    Namespace namespace;
    UserEntity user;
    Boolean multiple = false;
    
    /**
     * @return the id
     */
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="datatype_seq")
    @SequenceGenerator(name="datatype_seq", sequenceName="DATATYPE_SEQ", initialValue=50, allocationSize=50)
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
    @Column(nullable=false)
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
    @Column (length=4000)
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
    @ManyToOne(targetEntity = Namespace.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "namespaceid", foreignKey = @ForeignKey(name = "FK_VERIFY_NAMESPACE"))
    public Namespace getNamespace() {
        return namespace;
    }
    /**
     * @param namespace the namespace to set
     */
    public void setNamespace(Namespace namespace) {
        this.namespace = namespace;
    }
    /**
     * @return the user
     */
    @ManyToOne(targetEntity = UserEntity.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = true, name = "userid", foreignKey = @ForeignKey(name = "FK_VERIFY_USER"))
    public UserEntity getUser() {
        return user;
    }
    /**
     * @param user the user to set
     */
    public void setUser(UserEntity user) {
        this.user = user;
    }
    
    @Column
	public Boolean getMultiple() {
		return multiple;
	}
	public void setMultiple(Boolean multiple) {
		this.multiple = multiple;
	}

}
