package org.glygen.tablemaker.persistence.dataset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="datasetgrant")
public class Grant {
    
    Long id;
    String title;
    String identifier;
    String URL;
    String fundingOrganization;
    
    /**
     * @return the id
     */
    @Id
    @GeneratedValue
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
     * @return the title
     */
    @Column
    public String getTitle() {
        return title;
    }
    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }
    /**
     * @return the identifier
     */
    @Column
    public String getIdentifier() {
        return identifier;
    }
    /**
     * @param identifier the identifier to set
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
    /**
     * @return the uRL
     */
    @Column
    public String getURL() {
        return URL;
    }
    /**
     * @param uRL the uRL to set
     */
    public void setURL(String uRL) {
        URL = uRL;
    }
    /**
     * @return the fundingOrganization
     */
    @Column
    public String getFundingOrganization() {
        return fundingOrganization;
    }
    /**
     * @param fundingOrganization the fundingOrganization to set
     */
    public void setFundingOrganization(String fundingOrganization) {
        this.fundingOrganization = fundingOrganization;
    }
    
   

}
