package org.glygen.tablemaker.persistence;

import java.util.Collection;
import java.util.Date;

import org.glygen.tablemaker.persistence.glycan.GlycanInFile;
import org.glygen.tablemaker.persistence.glycan.UploadStatus;
import org.glygen.tablemaker.persistence.protein.GlycoproteinInFile;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name="file_upload")
public class BatchUploadEntity {
	Long id;
	UploadStatus status;
	Date startDate;
	Date accessedDate;
	UserEntity user;
	String filename;
	String format;
	Integer existingCount=0;
	Collection<UploadErrorEntity> errors;
	Collection<GlycanInFile> glycans;
	Collection<GlycoproteinInFile> glycoproteins;
	
	@Id
	@GeneratedValue
	@Column(name="file_upload_id")
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	@Enumerated(EnumType.STRING)
	@Column(name="status", nullable=false)
	public UploadStatus getStatus() {
		return status;
	}
	public void setStatus(UploadStatus status) {
		this.status = status;
	}
	
	
	@Column
	public Date getStartDate() {
		return startDate;
	}
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
	
	@Column
	public Date getAccessedDate() {
		return accessedDate;
	}
	public void setAccessedDate(Date accessedDate) {
		this.accessedDate = accessedDate;
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

    @OneToMany(mappedBy = "upload", cascade = CascadeType.ALL, orphanRemoval = true)
	public Collection<UploadErrorEntity> getErrors() {
		return errors;
	}

	public void setErrors(Collection<UploadErrorEntity> errors) {
		this.errors = errors;
		for (UploadErrorEntity err: errors) {
			err.setUpload(this);
		}
	}

	@Column
	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	@Column
	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	@OneToMany(mappedBy = "uploadFile", cascade = CascadeType.ALL, orphanRemoval = true)
	public Collection<GlycanInFile> getGlycans() {
		return glycans;
	}

	public void setGlycans(Collection<GlycanInFile> glycans) {
		this.glycans = glycans;
	}
	
	@OneToMany(mappedBy = "uploadFile", cascade = CascadeType.ALL, orphanRemoval = true)
	public Collection<GlycoproteinInFile> getGlycoproteins() {
		return glycoproteins;
	}
	
	public void setGlycoproteins(Collection<GlycoproteinInFile> glycoproteins) {
		this.glycoproteins = glycoproteins;
	}

	@Column
	public Integer getExistingCount() {
		return existingCount;
	}

	public void setExistingCount(Integer existingCount) {
		this.existingCount = existingCount;
	}
}
