package org.glygen.tablemaker.persistence.dao;

import java.util.List;
import java.util.Optional;

import org.glygen.tablemaker.persistence.NotificationEntity;
import org.glygen.tablemaker.persistence.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
	
	List<NotificationEntity> findByRecipientOrderByCreatedAtDesc(UserEntity recipient);
	
	Optional<NotificationEntity> findByIdAndRecipient (Long id, UserEntity recipient);

    long countByRecipientAndStatus(UserEntity recipient, String status);
}
