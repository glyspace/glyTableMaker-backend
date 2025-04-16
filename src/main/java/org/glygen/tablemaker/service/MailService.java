package org.glygen.tablemaker.service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.glygen.tablemaker.persistence.BatchUploadEntity;
import org.glygen.tablemaker.persistence.ErrorReportEntity;
import org.glygen.tablemaker.persistence.FeedbackEntity;
import org.glygen.tablemaker.persistence.UploadErrorEntity;
import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.UserError;
import org.glygen.tablemaker.persistence.dao.VerificationTokenRepository;
import org.glygen.tablemaker.util.RandomPasswordGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service(value="MailService")
public class MailService implements EmailManager {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    UserManager userManager;
    
    @Autowired
    VerificationTokenRepository tokenRepository;
    
    @Value("${spring.mail.username}")
    private String sender;
    
    @Value("${spring.mail.email}")
    private String email;
    
    @Value("${glygen.frontend.basePath}")
    String frontEndbasePath;
    
    @Value("${glygen.frontend.host}")
    String frontEndHost;
    
    @Value("${glygen.frontend.scheme}")
    String frontEndScheme;
    
    @Value("${glygen.frontend.emailVerificationPage}")
    String emailVerificationPage;
    
    @Value("${spring.file.uploaddirectory}")
	String uploadDir;
    
    PasswordEncoder passwordEncoder;
    
    public MailService(PasswordEncoder p) {
        this.passwordEncoder = p;
    }
    
    public void sendMessage(String recipientAddress, String subject, String body) {
        SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom(sender);
        email.setTo(recipientAddress);
        email.setSubject(subject);
        email.setText(body);
        mailSender.send(email);
    }

    @Override
    public void sendPasswordReminder(UserEntity user) {
        char[] pswd = RandomPasswordGenerator.generatePswd(5, 20, 1, 1, 1);
        String newPassword = new String(pswd);
        // encrypt the password
        //PasswordEncoder passwordEncoder =
        //        PasswordEncoderFactories.createDelegatingPasswordEncoder();
        String hashedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(hashedPassword);
        user.setTempPassword(true);
        userManager.createUser(user);
        
        final String recipientAddress = user.getEmail();
        final String subject = "GlyTableMaker: Password recovery";
            
        sendMessage(recipientAddress, subject, "Dear " + user.getFirstName() + " " + user.getLastName()
            + ", \n\nYour GlyTableMaker password has been reset to: \n\n" + new String(pswd) 
            + "\n\nPlease login to the application and change the password as soon as possible. \n\nThe GlyTableMaker team");
    }

    @Override
    public void sendVerificationToken(UserEntity user) {
        final String token = UUID.randomUUID().toString();
        userManager.createVerificationTokenForUser(user, token);
        String verificationURL = frontEndScheme + frontEndHost + frontEndbasePath + emailVerificationPage;
        final String recipientAddress = user.getEmail();
        final String subject = "GlyTableMaker account activation";
        final String confirmationUrl = verificationURL+ "/" + token;
        String message = "Dear " + user.getFirstName() + " " + user.getLastName();
        message += "\n\nThank you for signing up to the GlyTableMaker. If you have not created an account for your email address (" 
                + user.getEmail() + ") you can ignore this message. If you did create the account, please use the following link to activate the account:"
                + "\n\n" + confirmationUrl + "\n\nAlternatively, you can use the following activation code in the web frontend:"
                + "\n\n" + token + "\n\nThe GlyTableMaker";
          
       sendMessage(recipientAddress, subject, message);
        
    }

    @Override
    public void sendUserName(UserEntity user) {
        final String recipientAddress = user.getEmail();
        final String subject = "GlyTableMaker: Username recovery";
        
        sendMessage(recipientAddress, subject,  "Dear " + user.getFirstName() + " " + user.getLastName()
            + ", \n\n The Username for your account associated with this email is: \n\n" + user.getUsername()
            + "\n\n If you did not request your username, please ignore this email. \n\n "
            + "The GlyTableMaker team");
        
    }

    @Override
    public void sendEmailChangeNotification(UserEntity user) {
        final String recipientAddress = user.getEmail();
        final String subject = "GlyTableMaker: Email Change Notification";
            
        sendMessage(recipientAddress, subject,  "Dear " + user.getFirstName() + " " + user.getLastName()
            + ", \n\nYour GlyTableMaker account email has been changed. If you have not made this change, please contact us at " + email);
    }

	@Override
	public void sendErrorReport(UserError error, String... emails) {
		if (emails == null) {
            throw new IllegalArgumentException("email list cannot be null");
        }
		String subject="";
		String message="";
		if (error instanceof BatchUploadEntity) {
			BatchUploadEntity batchUploadEntity = (BatchUploadEntity) error;
			subject = "GlyTableMaker Upload Error: UploadId [" + batchUploadEntity.getId() + "]";
			message = "Upload process [" + batchUploadEntity.getId() + "] that started at " + 
        		batchUploadEntity.getStartDate() + " is still processing!";
		} else if (error instanceof UploadErrorEntity) {
			UploadErrorEntity uploadErrorEntity = (UploadErrorEntity) error;
			subject = "GlyTableMaker Upload Error: ErrorId [" + uploadErrorEntity.getId() + "]";
	        message = "Error message: " + uploadErrorEntity.getMessage() + "\nPosition: " + 
	        		(uploadErrorEntity.getPosition() !=null ? uploadErrorEntity.getPosition() : "unknown")+ 
	        		"\nSequence: " + uploadErrorEntity.getSequence() + "\nFile: " + 
	        		uploadDir + File.separator + uploadErrorEntity.getUpload().getId() + File.separator + uploadErrorEntity.getUpload().getFilename() +
	        		"\nUser: " + uploadErrorEntity.getUpload().getUser().getUsername();
		}
        
        for (String email: emails) {
            sendMessage(email, subject, message);
         }
	}
	
	@Override
    public void sendFeedback(FeedbackEntity feedback, String... emails) {
        if (emails == null) {
            throw new IllegalArgumentException("email list cannot be null");
        }
        String subject = "GlyTableMaker " + feedback.getSubject();
        String message = "Feedback received for page: " + feedback.getPage() + "\nwith the message: " + feedback.getMessage();
        message += "\nFrom: " + feedback.getFirstName() + 
                " " + (feedback.getLastName() == null || feedback.getLastName().equals("not given") ? "" : feedback.getLastName()) + "\nEmail: " + feedback.getEmail();
        for (String email: emails) {
           sendMessage(email, subject, message);
        }
        
    }

    @Override
    public void sendFeedbackNotice(FeedbackEntity feedback) {
        String subject = "Feedback received";
        String message = "Your feedback for GlyTableMaker has been recorded. Thank you!";
        message += "\n\nFeedback received for page: " + feedback.getPage() + "\nwith the message: " + feedback.getMessage();
        message += "\nFrom: " + feedback.getFirstName() + 
                " " + (feedback.getLastName() == null || feedback.getLastName().equals("not given") ? "" : feedback.getLastName()) + "\nEmail: " + feedback.getEmail();
       
        sendMessage(feedback.getEmail(), subject, message);
    }

	@Override
	public void sendErrorReport(ErrorReportEntity error, String... emails) {
		if (emails == null) {
            throw new IllegalArgumentException("email list cannot be null");
        }
        String subject = "GlyTableMaker System Error";
        String message = "A system error occurred on " + error.getDateReported() + " with the following message " 
        		+ error.getMessage() + "\nDetails (if any) " + error.getDetails() + "\nGithub ticket created (if any): " 
        		+ error.getTicketUrl(); 
        
        for (String email: emails) {
            sendMessage(email, subject, message);
         }
	}

}
