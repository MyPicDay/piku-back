package com.pikume.back.social.domain.like;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.pikume.back.global.entity.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "likes", uniqueConstraints = {
		@UniqueConstraint(name = "uk_user_diary", columnNames = { "user_id", "diary_id" })
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Like extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false, length = 36)
	private String userId;

	@Column(name = "diary_id", nullable = false)
	private Long diaryId;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@Builder
	public Like(String userId, Long diaryId) {
		this.userId = userId;
		this.diaryId = diaryId;
	}

	public void cancel() {
		this.deletedAt = LocalDateTime.now();
	}

	public void reactivate() {
		this.deletedAt = null;
	}

	public boolean isActive() {
		return deletedAt == null;
	}
}
