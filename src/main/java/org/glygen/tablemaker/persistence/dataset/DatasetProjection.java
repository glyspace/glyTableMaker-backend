package org.glygen.tablemaker.persistence.dataset;

import java.util.Date;

import org.glygen.tablemaker.persistence.UserEntity;

public interface DatasetProjection {
	Long getDatasetId();
	String getName();
	String getDescription();
	UserEntity getUser();
	Date getDateCreated();
	String getDatasetIdentifier();
}
