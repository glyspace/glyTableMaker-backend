package org.glygen.tablemaker.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.glygen.tablemaker.controller.DataController;
import org.glygen.tablemaker.exception.BatchUploadException;
import org.glygen.tablemaker.exception.DuplicateException;
import org.glygen.tablemaker.persistence.BatchUploadEntity;
import org.glygen.tablemaker.persistence.UploadErrorEntity;
import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.dao.GlycanRepository;
import org.glygen.tablemaker.persistence.glycan.Glycan;
import org.glygen.tablemaker.util.GlytoucanUtil;
import org.glygen.tablemaker.view.ExcelFileWrapper;
import org.glygen.tablemaker.view.GlycanView;
import org.glygen.tablemaker.view.SequenceFormat;
import org.glygen.tablemaker.view.SuccessResponse;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncServiceImpl implements AsyncService {
	 static Logger logger = org.slf4j.LoggerFactory.getLogger(AsyncServiceImpl.class);
	
	final private GlycanRepository glycanRepository;
	final private GlycanManager glycanManager;
	
	@Value("${spring.file.imagedirectory}")
    String imageLocation;
	
	public AsyncServiceImpl(GlycanRepository repository, GlycanManager glycanManager) {
		this.glycanRepository = repository;
		this.glycanManager = glycanManager;
	}

	@Override
	@Async("GlygenAsyncExecutor")
	public CompletableFuture<SuccessResponse<BatchUploadEntity>> addGlycanFromTextFile(byte[] contents, 
			BatchUploadEntity upload, UserEntity user, SequenceFormat format,
			String delimeter, String tag) {
		try {
            ByteArrayInputStream stream = new   ByteArrayInputStream(contents);
            String fileAsString = IOUtils.toString(stream, StandardCharsets.UTF_8.name());
            String[] structures = fileAsString.split(delimeter);
            int count = 0;
            int countSuccess = 0;
            List<UploadErrorEntity> errors = new ArrayList<>();
            List<Glycan> allGlycans = new ArrayList<>();
            for (String sequence: structures) {
                if (sequence == null || sequence.trim().isEmpty())
                    continue;
                count++;
                try {
                	//  parse and add glycan
                	GlycanView g = new GlycanView();
                	g.setFormat(format);
                	g.setSequence(sequence);
                	Glycan glycan = new Glycan();
                	DataController.parseAndRegisterGlycan(glycan, g, glycanRepository, user);
                	// save the glycan
                    glycan.setDateCreated(new Date());
                    glycan.setUser(user);
                    Glycan added = glycanManager.addUploadToGlycan(glycan, upload, true, user);
                    allGlycans.add(added);
                    if (added != null) {
                        BufferedImage t_image = DataController.createImageForGlycan(added);
                        if (t_image != null) {
                            String filename = added.getGlycanId() + ".png";
                            //save the image into a file
                            logger.debug("Adding image to " + imageLocation);
                            File imageFile = new File(imageLocation + File.separator + filename);
                            try {
                                ImageIO.write(t_image, "png", imageFile);
                            } catch (IOException e) {
                                logger.error("could not write cartoon image to file", e);
                            }
                        } else {
                            logger.warn ("Glycan image cannot be generated for glycan " + added.getGlycanId());
                        }
                    }
                	countSuccess++;
                } catch (DuplicateException e) {
                	//errors.add(new UploadErrorEntity(count+"", "duplicate", sequence));
                	if (e.getDuplicate() != null && e.getDuplicate() instanceof Glycan) {
                		Glycan existing = (Glycan) e.getDuplicate();
                		if (allGlycans.contains(existing)) {
                			// duplicate within the file
                			// error
                			errors.add(new UploadErrorEntity(count+"", "This sequence is a duplicate in the file", sequence));
                		} else {
                			glycanManager.addUploadToGlycan(existing, upload, false, user);
                			allGlycans.add(existing);
                		}
                		
                	}
                } catch (Exception e) {
                	errors.add(new UploadErrorEntity(count+"", e.getMessage(), sequence));
                }
            }
            if (tag != null && !tag.trim().isEmpty()) {
            	glycanManager.addTagToGlycans(allGlycans, tag, user);
            }
            if (!errors.isEmpty()) {
            	return CompletableFuture.failedFuture(new BatchUploadException("There are errors in the file", errors));
            }
            return CompletableFuture.completedFuture (new SuccessResponse<BatchUploadEntity>(upload, countSuccess + " out of " + count + " glycans are added successfully"));
		} catch (IOException e) {
			List<UploadErrorEntity> errors = new ArrayList<>();
			errors.add(new UploadErrorEntity(null, "File is not valid. Reason: " + e.getMessage(), null));
            return CompletableFuture.failedFuture(new BatchUploadException("File is not valid.", errors));
        }
	}

	@Override
	@Async("GlygenAsyncExecutor")
	public CompletableFuture<SuccessResponse<BatchUploadEntity>> addGlycanFromExcelFile(File file, BatchUploadEntity upload,
			UserEntity user, ExcelFileWrapper excelParameters, String tag) {
		
		try {
			List<UploadErrorEntity> errors = new ArrayList<>();
            List<Glycan> allGlycans = new ArrayList<>();
            GlytoucanUtil glytoucanUtil = GlytoucanUtil.getInstance();
			Workbook workbook = WorkbookFactory.create(file);
			int sheetNo = 0;
			int columnNo = 0;
			int rowNo = 0;
			if (excelParameters != null) {
				if (excelParameters.getSheetNumber() != null) 
					sheetNo = excelParameters.getSheetNumber()-1;   // 0 based index
	 			if (excelParameters.getColumnNo() != null) 
					columnNo = excelParameters.getColumnNo()-1;
				if (excelParameters.getStartRow() != null) 
					rowNo = excelParameters.getStartRow()-1;
			}
			Sheet sheet = workbook.getSheetAt(sheetNo);
			Iterator<Row> rowIterator = sheet.iterator();
			boolean started = false;
			int count = 0;
            int countSuccess = 0;
	        while (rowIterator.hasNext()) {
	            Row row = rowIterator.next();
	            if (row.getRowNum() == rowNo) {
	            	started = true;
	            }
	            if (started) {
	            	count++;
	            	Cell glytoucanIdCell = row.getCell(columnNo);
	            	String sequence = glytoucanUtil.retrieveGlycan(glytoucanIdCell.getStringCellValue().trim());
	            	if (sequence == null) {
	            		errors.add(new UploadErrorEntity(count+"", "GlyTouCanId " + glytoucanIdCell.getStringCellValue() + " is not valid!", null));
	            	}
	                try {
	                	//  parse and add glycan
	                	GlycanView g = new GlycanView();
	                	g.setFormat(SequenceFormat.WURCS);
	                	g.setSequence(sequence);
	                	Glycan glycan = new Glycan();
	                	DataController.parseAndRegisterGlycan(glycan, g, glycanRepository, user);
	                	// save the glycan
	                    glycan.setDateCreated(new Date());
	                    glycan.setUser(user);
	                    Glycan added = glycanManager.addUploadToGlycan(glycan, upload, true, user);
	                    allGlycans.add(added);
	                    if (added != null) {
	                        BufferedImage t_image = DataController.createImageForGlycan(added);
	                        if (t_image != null) {
	                            String filename = added.getGlycanId() + ".png";
	                            //save the image into a file
	                            logger.debug("Adding image to " + imageLocation);
	                            File imageFile = new File(imageLocation + File.separator + filename);
	                            try {
	                                ImageIO.write(t_image, "png", imageFile);
	                            } catch (IOException e) {
	                                logger.error("could not write cartoon image to file", e);
	                            }
	                        } else {
	                            logger.warn ("Glycan image cannot be generated for glycan " + added.getGlycanId());
	                        }
	                    }
	                	countSuccess++;
	                } catch (DuplicateException e) {
	                	if (e.getDuplicate() != null && e.getDuplicate() instanceof Glycan) {
	                		Glycan existing = (Glycan) e.getDuplicate();
	                		if (allGlycans.contains(existing)) {
	                			// duplicate within the file
	                			// error
	                			errors.add(new UploadErrorEntity(count+"", "This sequence is a duplicate in the file", sequence));
	                		} else {
	                			glycanManager.addUploadToGlycan(existing, upload, false, user);
	                			allGlycans.add(existing);
	                		}
	                		
	                	}
	                } catch (Exception e) {
	                	errors.add(new UploadErrorEntity(count+"", e.getMessage(), sequence));
	                }
	            	
	            }
	        }
	        if (tag != null && !tag.trim().isEmpty()) {
            	glycanManager.addTagToGlycans(allGlycans, tag, user);
            }
            if (!errors.isEmpty()) {
            	return CompletableFuture.failedFuture(new BatchUploadException("There are errors in the file", errors));
            }
	        
	        return CompletableFuture.completedFuture (new SuccessResponse<BatchUploadEntity>(upload, countSuccess + " out of " + count + " glycans are added successfully"));
		} catch (Exception e) {
			List<UploadErrorEntity> errors = new ArrayList<>();
			errors.add(new UploadErrorEntity(null, "File is not valid. Reason: " + e.getMessage(), null));
            return CompletableFuture.failedFuture(new BatchUploadException("File is not valid.", errors));
		}
	}

}
