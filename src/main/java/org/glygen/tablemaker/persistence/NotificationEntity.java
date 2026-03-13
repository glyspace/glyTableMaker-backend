package org.glygen.tablemaker.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "notifications")
public class NotificationEntity {
	    @Id
	    private Long id;

	    @ManyToOne(targetEntity = UserEntity.class, fetch = FetchType.EAGER)
		@JoinColumn(nullable = false, name = "userid", foreignKey = @ForeignKey(name = "FK_VERIFY_USER"))
	    private UserEntity recipient;

	    @Column
	    private Long senderId;

	    @Column
	    private String type;

	    @Column
	    private String title;

	    @Column(length=4000)
	    private String message;

	    @Column
	    private String status;

	    @Column(columnDefinition = "jsonb")
	    private String metadata;

	    @Column
	    private Instant createdAt;

	    @Column
	    private Instant readAt;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public UserEntity getRecipient() {
			return recipient;
		}

		public void setRecipient(UserEntity recipient) {
			this.recipient = recipient;
		}


		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}

		public String getMetadata() {
			return metadata;
		}

		public void setMetadata(String metadata) {
			this.metadata = metadata;
		}

		public Instant getCreatedAt() {
			return createdAt;
		}

		public void setCreatedAt(Instant createdAt) {
			this.createdAt = createdAt;
		}

		public Instant getReadAt() {
			return readAt;
		}

		public void setReadAt(Instant readAt) {
			this.readAt = readAt;
		}

		public Long getSenderId() {
			return senderId;
		}

		public void setSenderId(Long senderId) {
			this.senderId = senderId;
		}

}
