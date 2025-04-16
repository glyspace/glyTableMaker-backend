package org.glygen.tablemaker.service;

import org.glygen.tablemaker.persistence.ErrorReportEntity;
import org.glygen.tablemaker.persistence.FeedbackEntity;
import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.UserError;

public interface EmailManager {
	void sendPasswordReminder (UserEntity user);
	void sendVerificationToken(UserEntity user);
	void sendUserName(UserEntity user);
	void sendEmailChangeNotification (UserEntity user);
	void sendFeedbackNotice (FeedbackEntity feedback);
    void sendFeedback(FeedbackEntity feedback, String... emails);
	void sendErrorReport(UserError error, String...emails);
	void sendErrorReport(ErrorReportEntity error, String ... emails);
}
