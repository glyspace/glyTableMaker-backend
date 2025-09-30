package org.glygen.tablemaker.persistence.dataset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Publication {
	Long id;
	private String authors;
    private Integer pubmedId = null;
    private String doiId = null;
    private String title = null;
    private String journal = null;
    private String startPage = null;
    private String endPage = null;
    private String volume = null;
    private Integer year = null;
    private String number = null;
    
    
    @Id
    @GeneratedValue
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
	/**
	 * @return the authors
	 */
    @Column
	public String getAuthors() {
        return authors;
    }
	/**
	 * @param authors the authors to set
	 */
	public void setAuthors(String authors) {
        this.authors = authors;
    }

    /**
     * @return the pubmedId
     */
	@Column
    public Integer getPubmedId() {
        return pubmedId;
    }

    /**
     * @param pubmedId the pubmedId to set
     */
    public void setPubmedId(Integer pubmedId) {
        this.pubmedId = pubmedId;
    }

    /**
     * @return the doiId
     */
    @Column
    public String getDoiId() {
        return doiId;
    }

    /**
     * @param doiId the doiId to set
     */
    public void setDoiId(String doiId) {
        this.doiId = doiId;
    }

    /**
     * @return the title
     */
    @Column(length = 4000)
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
     * @return the journal
     */
    @Column
    public String getJournal() {
        return journal;
    }

    /**
     * @param journal the journal to set
     */
    public void setJournal(String journal) {
        this.journal = journal;
    }

    /**
     * @return the startPage
     */
    @Column
    public String getStartPage() {
        return startPage;
    }

    /**
     * @param startPage the startPage to set
     */
    public void setStartPage(String startPage) {
        this.startPage = startPage;
    }

    /**
     * @return the endPage
     */
    @Column
    public String getEndPage() {
        return endPage;
    }

    /**
     * @param endPage the endPage to set
     */
    public void setEndPage(String endPage) {
        this.endPage = endPage;
    }

    /**
     * @return the volume
     */
    @Column
    public String getVolume() {
        return volume;
    }

    /**
     * @param volume the volume to set
     */
    public void setVolume(String volume) {
        this.volume = volume;
    }

    /**
     * @return the year
     */
    @Column
    public Integer getYear() {
        return year;
    }

    /**
     * @param year the year to set
     */
    public void setYear(Integer year) {
        this.year = year;
    }

    /**
     * @return the number
     */
    @Column
    public String getNumber() {
        return number;
    }

    /**
     * @param number the number to set
     */
    public void setNumber(String number) {
        this.number = number;
    }
	
    
    @Override
    public boolean equals(Object obj) {
    	if (obj instanceof Publication) {
    		if (this.pubmedId != null) {
    			return this.pubmedId.equals(((Publication) obj).getPubmedId());
    		} else if (this.doiId != null) {
    			return this.doiId.equalsIgnoreCase(((Publication) obj).getDoiId());
    		}
    	}
    	
    	return super.equals(obj);
    }
    
    @Override
    public int hashCode() {
    	if (this.pubmedId != null) return pubmedId.hashCode();
    	else if (this.doiId != null) return doiId.hashCode();
    	return super.hashCode();
    }

}
