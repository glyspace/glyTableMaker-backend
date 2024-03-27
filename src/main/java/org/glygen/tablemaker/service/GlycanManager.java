package org.glygen.tablemaker.service;

import java.util.Collection;

import org.glygen.tablemaker.persistence.BatchUploadEntity;
import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.glycan.Glycan;

public interface GlycanManager {
	void addTagToGlycans (Collection<Glycan> glycans, String tag, UserEntity user);
	void deleteUploadEntity(BatchUploadEntity upload, UserEntity user);
}
