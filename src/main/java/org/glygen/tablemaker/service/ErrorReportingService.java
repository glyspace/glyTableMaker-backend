package org.glygen.tablemaker.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.glygen.tablemaker.persistence.BatchUploadEntity;
import org.glygen.tablemaker.persistence.ErrorReportEntity;
import org.glygen.tablemaker.persistence.UploadErrorEntity;
import org.glygen.tablemaker.persistence.UserError;
import org.glygen.tablemaker.persistence.dao.ErrorReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;

@Transactional
@Service
public class ErrorReportingService {
	
	final static Logger logger = LoggerFactory.getLogger(ErrorReportingService.class);
	
	final private ErrorReportRepository errorReportRepository;
	final private EmailManager emailManager;

	@Value ("${github.token}")
	String githubToken;
	
	@Value ("${github.issuesUrl}")
	String githubIssuesUrl;
	
	@Value ("${github.repoUrl}")
	String githubRepoUrl;
	
	@Value ("${github.assignee}")
	String githubAssignee;
	
	@Value("${spring.file.uploaddirectory}")
	String uploadDir;
	
	List<String> emails = new ArrayList<String>();
	CloseableHttpClient httpClient = HttpClients.createDefault();
	
	public ErrorReportingService(ErrorReportRepository errorReportRepository, EmailManager emailManager) {
		this.errorReportRepository = errorReportRepository;
		this.emailManager = emailManager;
		
		// retrieve admin emails
        try {
            Resource classificationNamespace = new ClassPathResource("adminemails.txt");
            final InputStream inputStream = classificationNamespace.getInputStream();
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                emails.add(line.trim());
            }
        } catch (Exception e) {
            logger.error("Cannot locate admin emails", e);
            try {
            	createIssue ("Cannot locate admin emails", "adminemails.txt is not found in the classpath. Exception: " + e.getMessage(), "SettingsError");
            } catch (Exception e1) {
            	logger.error("could not create the issue in Github. Reason " + e1.getMessage(), e1);
            }
        }
	}

	public String createIssue(String title, String body, String label) throws Exception {
		String issueUrl = null;
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			HttpPost httpPost = new HttpPost(githubIssuesUrl);
			httpPost.setHeader("Authorization", "Bearer " + githubToken);
			httpPost.setHeader("Accept", "application/vnd.github.v3+json");
			
			String[] assignees = githubAssignee.split(",");
			String assigneeString = "";
			for (String assignee: assignees) {
				assigneeString += "\"" + assignee.trim() + "\",";
			}
			assigneeString = assigneeString.substring(0, assigneeString.length()-1);
			
			String cleanedBody = escapeJsonString(body);

			String json = String.format("{\"title\": \"%s\", \"body\": \"%s\", \"assignees\": [%s], \"type\": \"bug\", \"labels\": [\"%s\"]}", title, cleanedBody, assigneeString, label);
			StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);
			httpPost.setEntity(entity);
			ObjectMapper objectMapper = new ObjectMapper();
			try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
				String responseBody = EntityUtils.toString(response.getEntity());
				JsonNode jsonNode = objectMapper.readTree(responseBody);
				if (jsonNode.has("url")) {
					issueUrl = jsonNode.get("url").asText();
					if (issueUrl.contains("/"))
						issueUrl = githubRepoUrl + issueUrl.substring(issueUrl.lastIndexOf("/"));
				}
				if (response.getStatusLine().getStatusCode() < 400) {
					logger.info("Issue created successfully: " + responseBody);
				} else {
					logger.error("Failed to create issue: " + response.getStatusLine().getStatusCode());
				}		
			}
		}
		return issueUrl;
	}
	

	private String escapeJsonString(String str) {
		return str.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\n", "\\n")
				.replace("\t", " ")
				.replace("\r", "\\r");
	}
	
	public void reportUserError (UserError error) {
		emailManager.sendErrorReport(error, emails.toArray(new String[0]));
		String subject = "User Reported Issue: ";
		String message = "";
		if (error instanceof BatchUploadEntity) {
			BatchUploadEntity batchUploadEntity = (BatchUploadEntity) error;
			subject += "GlyTableMaker Upload Error: UploadId [" + batchUploadEntity.getId() + "]";
			message = "Upload process [" + batchUploadEntity.getId() + "] that started at " + 
        		batchUploadEntity.getStartDate() + " is still processing!";
		} else if (error instanceof UploadErrorEntity) {
			UploadErrorEntity uploadErrorEntity = (UploadErrorEntity) error;
			subject += "GlyTableMaker Upload Error: ErrorId [" + uploadErrorEntity.getId() + "]";
	        message = "Error message: " + uploadErrorEntity.getMessage() + "\nPosition: " + 
	        		(uploadErrorEntity.getPosition() !=null ? uploadErrorEntity.getPosition() : "unknown")+ 
	        		"\nSequence: " + uploadErrorEntity.getSequence() + "\nFile: " + 
	        		uploadDir + File.separator + uploadErrorEntity.getUpload().getId() + File.separator + uploadErrorEntity.getUpload().getFilename() +
	        		"\nUser: " + uploadErrorEntity.getUpload().getUser().getUsername();
		}
		// create ticket in Github
		try {
			createIssue (subject, message, "User Reported");
		} catch (Exception e) {
			logger.error("could not create the issue in Github. Reason " + e.getMessage(), e);
		}
	}	

	public void reportError (ErrorReportEntity error) {
		List<ErrorReportEntity> existingReports = errorReportRepository.findByMessageAndDateReported(error.getMessage(), error.getDateReported());
		if (existingReports != null && existingReports.size() > 0) {
			// already reported this error on this date, ignoring
			logger.info ("Already Reported the error, ignoring for now: " + error.getMessage() + " Date: " + error.getDateReported());
		} else {
			// create ticket in Github
			try {
				String issueUrl = createIssue (error.getMessage(), error.getDetails(), error.getTicketLabel());
				if (issueUrl != null) {
					error.setTicketUrl(issueUrl);
				}
			} catch (Exception e) {
				logger.error("could not create the issue in Github. Reason " + e.getMessage(), e);
			}
			// send emails and create a ticket in Github
			if (!emails.isEmpty()) {
				emailManager.sendErrorReport(error, emails.toArray(new String[0]));
			}
			errorReportRepository.save(error);
		}
	}
}
