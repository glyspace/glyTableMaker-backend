package org.glygen.tablemaker.service;

import java.util.concurrent.CompletableFuture;

import org.glygen.tablemaker.persistence.BatchUploadEntity;
import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.view.SequenceFormat;
import org.glygen.tablemaker.view.SuccessResponse;

public interface AsyncService {
	CompletableFuture<SuccessResponse> addGlycanFromTextFile(byte[] contents,
			BatchUploadEntity upload,
            UserEntity user, SequenceFormat format, String delimeter);
}
