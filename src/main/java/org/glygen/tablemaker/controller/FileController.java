package org.glygen.tablemaker.controller;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import org.glygen.tablemaker.exception.UploadNotFinishedException;
import org.glygen.tablemaker.view.FileWrapper;
import org.glygen.tablemaker.view.ResumableFileInfo;
import org.glygen.tablemaker.view.ResumableInfoStorage;
import org.glygen.tablemaker.view.SuccessResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/file")
public class FileController {
	
	@Value("${spring.file.uploaddirectory}")
	String uploadDir;
	
	@Operation(summary = "Upload file", security = { @SecurityRequirement(name = "bearer-key") })
	@RequestMapping(value = "/upload", method=RequestMethod.POST, 
	        consumes= {"application/octet-stream"})
	public ResponseEntity<SuccessResponse> uploadFile(
	        HttpEntity<byte[]> requestBody,
            @RequestParam("resumableFilename") String resumableFilename,
            @RequestParam ("resumableRelativePath") String resumableRelativePath,
            @RequestParam ("resumableTotalChunks") String resumableTotalChunks,
            @RequestParam("resumableChunkSize") int resumableChunkSize,
            @RequestParam("resumableChunkNumber") int resumableChunkNumber,
            @RequestParam("resumableTotalSize") long resumableTotalSize,
            @RequestParam("resumableIdentifier") String resumableIdentifier
    ) throws IOException, InterruptedException {
		 
        ResumableFileInfo info = ResumableInfoStorage.getInstance().get(resumableIdentifier);
        if (info == null) {
            String extension = resumableFilename.substring(resumableFilename.lastIndexOf(".")+1);
        	String uniqueFileName = System.currentTimeMillis() + "." +  extension;
            String resumableFilePath = new File(uploadDir, uniqueFileName).getAbsolutePath() + ".temp";
        	info = new ResumableFileInfo();
            info.resumableChunkSize = resumableChunkSize;
            info.resumableIdentifier = resumableIdentifier;
            info.resumableTotalSize = resumableTotalSize;
            info.resumableFilename = resumableFilename;
            info.resumableRelativePath = resumableRelativePath;
            info.resumableFilePath = resumableFilePath;
        	ResumableInfoStorage.getInstance().add(info);
        } 
        
		RandomAccessFile raf = new RandomAccessFile(info.resumableFilePath, "rw");

        //Seek to position
        raf.seek((resumableChunkNumber - 1) * (long)resumableChunkSize);
        //byte[] payload = file.getBytes();
        byte[] payload = requestBody.getBody();
        InputStream is = new ByteArrayInputStream(payload);
        long content_length = payload.length;
        long read = 0;
        byte[] bytes = new byte[1024 * 100];
        while(read < content_length) {
            int r = is.read(bytes);
            if (r < 0)  {
                break;
            }
            raf.write(bytes, 0, r);
            read += r;
        }
        raf.close();
        
        info.uploadedChunks.add(new ResumableFileInfo.ResumableChunkNumber(resumableChunkNumber));
        
        FileWrapper file = new FileWrapper();
        file.setIdentifier(info.resumableFilePath.substring(info.resumableFilePath.lastIndexOf(File.separator) + 1));
        file.setOriginalName(resumableFilename);
        file.setFileFolder(uploadDir);
        file.setFileSize(read);
        file.setExtension(file.getIdentifier().substring(file.getIdentifier().lastIndexOf(".")+1));
        
        if (info.checkIfUploadFinished()) { //Check if all chunks uploaded, and change filename
            ResumableInfoStorage.getInstance().remove(info);
            int index = info.resumableFilePath.indexOf(".temp") == -1 ? info.resumableFilePath.length() : info.resumableFilePath.indexOf(".temp");
            file.setIdentifier(info.resumableFilePath.substring(info.resumableFilePath.lastIndexOf(File.separator) + 1, index));
            return new ResponseEntity<>(new SuccessResponse(file, "file uploaded"), HttpStatus.OK);
        } else {
        	return new ResponseEntity<>(new SuccessResponse(file, "file uploaded"), HttpStatus.ACCEPTED);
        }
	}
	
	@Operation(summary = "Check status for upload file", security = { @SecurityRequirement(name = "bearer-key") })
	@GetMapping("/upload")
	public ResponseEntity<SuccessResponse> resumeUpload (
			@RequestParam("resumableFilename") String resumableFilename,
			@RequestParam ("resumableRelativePath") String resumableRelativePath,
            @RequestParam ("resumableTotalChunks") String resumableTotalChunks,
            @RequestParam("resumableChunkSize") int resumableChunkSize,
            @RequestParam("resumableCurrentChunkSize") int resumableCurrentChunkSize,
            @RequestParam("resumableChunkNumber") int resumableChunkNumber,
            @RequestParam("resumableTotalSize") long resumableTotalSize,
            @RequestParam("resumableType") String resumableType,
            @RequestParam("resumableIdentifier") String resumableIdentifier) {

		
        ResumableFileInfo info = ResumableInfoStorage.getInstance().get(resumableIdentifier);
        if (info == null || !info.valid()) {
        	if (info != null) ResumableInfoStorage.getInstance().remove(info);
        	if (resumableChunkNumber != 1) {
        	    throw new IllegalArgumentException("file identifier is not valid");
        	} else {
        	    throw new UploadNotFinishedException("Not found");  // this will return HttpStatus no_content 204
        	}
        }
        if (info.uploadedChunks.contains(new ResumableFileInfo.ResumableChunkNumber(resumableChunkNumber))) {
        	return new ResponseEntity<>(new SuccessResponse(resumableChunkNumber, "file uploaded"), HttpStatus.OK);
        } else {
            throw new UploadNotFinishedException("Not found");  // this will return HttpStatus no_content 204
        }
    }
	
	public static ResponseEntity<Resource> download (File file, String originalName, String fileId) {
        FileSystemResource r = new FileSystemResource(file);
        MediaType mediaType = MediaTypeFactory
                .getMediaType(r)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
       
        
        HttpHeaders respHeaders = new HttpHeaders();
        respHeaders.setContentType(mediaType);
        respHeaders.setContentLength(file.length());
        
        String add = (fileId == null ? "" : ":" + fileId);

        ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                .filename(originalName + add)
                .build();

        respHeaders.set(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
        respHeaders.set(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,"Content-Disposition");
        
        return new ResponseEntity<Resource>(
                r, respHeaders, HttpStatus.OK
        );
    }

}
