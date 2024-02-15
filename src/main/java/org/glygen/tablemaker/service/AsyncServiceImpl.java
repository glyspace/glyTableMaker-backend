package org.glygen.tablemaker.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.glygen.tablemaker.controller.DataController;
import org.glygen.tablemaker.exception.DuplicateException;
import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.dao.GlycanRepository;
import org.glygen.tablemaker.persistence.glycan.Glycan;
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
	
	@Value("${spring.file.imagedirectory}")
    String imageLocation;
	
	public AsyncServiceImpl(GlycanRepository repository) {
		this.glycanRepository = repository;
	}

	@Override
	@Async("GlygenAsyncExecutor")
	public CompletableFuture<SuccessResponse> addGlycanFromTextFile(byte[] contents, UserEntity user, SequenceFormat format,
			String delimeter) {
		try {
            ByteArrayInputStream stream = new   ByteArrayInputStream(contents);
            String fileAsString = IOUtils.toString(stream, StandardCharsets.UTF_8.name());
            String[] structures = fileAsString.split(delimeter);
            int count = 0;
            int countSuccess = 0;
            StringBuffer errorMessage = new StringBuffer();
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
                    Glycan added = glycanRepository.save(glycan);
                    
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
                	errorMessage.append("Row " + count + " is a duplicate.");
                } catch (Exception e) {
                	errorMessage.append("Row " + count + " is not added. Reason: " + e.getMessage());
                }
            }
            if (!errorMessage.toString().isEmpty()) {
            	return CompletableFuture.failedFuture(new IllegalArgumentException("There are errors in the file: " + errorMessage.toString()));
            }
            return CompletableFuture.completedFuture (new SuccessResponse(structures, countSuccess + " out of " + count + " glycans are added successfully"));
		} catch (IOException e) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("File is not valid. Reason: " + e.getMessage()));
        }
	}

}
