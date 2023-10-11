package org.glygen.tablemaker.service;

import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.VerificationToken;

public interface UserManager {

	UserEntity getUserByToken(String verificationToken);

	VerificationToken getVerificationToken(String VerificationToken);

	void createVerificationTokenForUser(UserEntity user, String token);

	VerificationToken generateNewVerificationToken(String existingVerificationToken);

	//String validateVerificationToken(String token);
	
	void deleteVerificationToken (String token);

	void createUser(UserEntity newUser);
	
	void deleteUser (UserEntity user);
	
	UserEntity recoverLogin(String email);
	
	void changePassword (UserEntity user, String newPassword);
	
	//void changeEmail (UserEntity user, String oldEmail, String newEmail);
	
	UserEntity getUserByUsername(String userName);
	
	//void cleanUpExpiredSignup ();

}