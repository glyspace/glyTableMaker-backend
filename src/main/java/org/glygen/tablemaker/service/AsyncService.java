package org.glygen.tablemaker.service;

import java.io.File;
import java.util.concurrent.CompletableFuture;

import org.glygen.tablemaker.persistence.BatchUploadEntity;
import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.protein.MultipleGlycanOrder;
import org.glygen.tablemaker.view.ExcelFileWrapper;
import org.glygen.tablemaker.view.SequenceFormat;
import org.glygen.tablemaker.view.SuccessResponse;

public interface AsyncService {
	CompletableFuture<SuccessResponse<BatchUploadEntity>> addGlycanFromTextFile(byte[] contents,
			BatchUploadEntity upload,
            UserEntity user, SequenceFormat format, String delimeter, String tag);

	CompletableFuture<SuccessResponse<BatchUploadEntity>> addGlycanFromExcelFile(File file, BatchUploadEntity saved,
			UserEntity user, ExcelFileWrapper excelParameters, String tag);

	CompletableFuture<SuccessResponse<BatchUploadEntity>> addGlycoproteinFromByonicFile(File file,
			BatchUploadEntity upload, UserEntity user, String delimeter, String tag, MultipleGlycanOrder multipleGlycanOrder);
}
