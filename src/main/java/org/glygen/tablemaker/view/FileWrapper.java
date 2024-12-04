package org.glygen.tablemaker.view;

import java.util.Date;

public class FileWrapper {
    
    String id;
    String identifier;
    String originalName;
    String fileFolder;
    String fileFormat;
    String extension;
    Long fileSize;
    String description;
    Date createdDate;
    ExcelFileWrapper excelParameters;
    
    /**
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }
    /**
     * @param identifier the identifier to set
     */
    public void setIdentifier(String indentifier) {
        this.identifier = indentifier;
    }
    /**
     * @return the originalName
     */
    public String getOriginalName() {
        return originalName;
    }
    /**
     * @param originalName the originalName to set
     */
    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }
    /**
     * @return the fileFolder
     */
    public String getFileFolder() {
        return fileFolder;
    }
    /**
     * @param fileFolder the fileFolder to set
     */
    public void setFileFolder(String fileFolder) {
        this.fileFolder = fileFolder;
    }
    /**
     * @return the fileFormat
     */
    public String getFileFormat() {
        return fileFormat;
    }
    /**
     * @param fileFormat the fileFormat to set
     */
    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }
    /**
     * @return the fileSize
     */
    public Long getFileSize() {
        return fileSize;
    }
    /**
     * @param fileSize the fileSize to set
     */
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
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
     * @return the id
     */
    public String getId() {
        return id;
    }
    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }
    /**
     * @return the createdDate
     */
    public Date getCreatedDate() {
        return createdDate;
    }
    /**
     * @param createdDate the createdDate to set
     */
    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }
    /**
     * @return the extension
     */
    public String getExtension() {
        return extension;
    }
    /**
     * @param extension the extension to set
     */
    public void setExtension(String extension) {
        this.extension = extension;
    }
	public ExcelFileWrapper getExcelParameters() {
		return excelParameters;
	}
	public void setExcelParameters(ExcelFileWrapper excelParameters) {
		this.excelParameters = excelParameters;
	}
}
