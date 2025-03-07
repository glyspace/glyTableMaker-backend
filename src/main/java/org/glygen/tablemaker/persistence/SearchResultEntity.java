package org.glygen.tablemaker.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;

@Entity
@Table(name="search_results")
public class SearchResultEntity {
	
	String searchKey;
	String idList;
	String input;

	/**
     * @return the searchKey
     */
    @Column(name="search_key", nullable=false, unique=true, length=4000)
    @NotEmpty
    @Id
    public String getSearchKey() {
        return searchKey;
    }
    /**
     * @param key the searchKey to set
     */
    public void setSearchKey(String key) {
        this.searchKey = key;
    }
    /**
     * @return the value
     */
    @Column(name="idlist", nullable=true)
    public String getIdList() {
        return idList;
    }
    /**
     * @param idList the list of ids to set
     */
    public void setIdList(String idList) {
        this.idList = idList;
    }

    /**
     * @return the input
     */
    @Column(name="input", nullable=false)
    @NotEmpty
    public String getInput() {
        return input;
    }

    /**
     * @param input the input to set
     */
    public void setInput(String input) {
        this.input = input;
    }
}
