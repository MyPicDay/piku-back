package com.pikume.back.notification.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.pikume.back.global.entity.BaseEntity;
import com.pikume.back.notification.domain.vo.NotificationType;

@Entity
@Table(name = "notification")
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class Notification extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String receiverId;

	@Column(name = "user_id", length = 36)
	private String senderId;

	@Enumerated(EnumType.STRING)
	private NotificationType type;

	@Column(name = "diary_id")
	private Long diaryId;

	private Boolean isRead;

	public Notification(String receiverId, String senderId, NotificationType type, Long diaryId) {
		this.receiverId = receiverId;
		this.senderId = senderId;
		this.type = type;
		this.diaryId = diaryId;
		this.isRead = false;
	}

	public void markAsRead() {
		this.isRead = true;
	}
}
