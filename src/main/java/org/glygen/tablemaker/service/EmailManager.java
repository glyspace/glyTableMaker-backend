package org.glygen.tablemaker.service;

import org.glygen.tablemaker.persistence.UserEntity;

public interface EmailManager {
	void sendPasswordReminder (UserEntity user);
	void sendVerificationToken(UserEntity user);
	void sendUserName(UserEntity user);
	void sendEmailChangeNotification (UserEntity user);
	//void sendFeedbackNotice (FeedbackEntity feedback);
    //void sendFeedback(FeedbackEntity feedback, String... emails);
}
