package org.glygen.tablemaker.persistence.dao;

import java.util.List;

import org.glygen.tablemaker.persistence.NotificationEntity;
import org.glygen.tablemaker.persistence.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
	
	List<NotificationEntity> findByRecipientOrderByCreatedAtDesc(UserEntity recipient);

    long countByRecipientAndStatus(UserEntity recipient, String status);
}
