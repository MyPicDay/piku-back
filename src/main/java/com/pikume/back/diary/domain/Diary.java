package com.pikume.back.diary.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import com.pikume.back.global.entity.BaseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table
@NoArgsConstructor
@Getter
public class Diary extends BaseEntity {

	private static final int MAX_CONTENT_LENGTH = 500;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(length = 500)
	private String content;

	@Enumerated(EnumType.STRING)
	private DiaryVisibility status;

	private LocalDate date;

	@Column(name = "user_id", length = 36)
	private String userId;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	public Diary(String content, DiaryVisibility status, LocalDate date, String userId) {
		validateContent(content);
		validateRequired(status, "공개범위는 필수입니다.");
		validateRequired(date, "일기 날짜는 필수입니다.");
		if (userId == null || userId.isBlank()) {
			throw new IllegalArgumentException("작성자 식별자는 필수입니다.");
		}
		this.content = content;
		this.status = status;
		this.date = date;
		this.userId = userId;
	}

	public static Diary create(String content, DiaryVisibility status, LocalDate date, String userId) {
		return new Diary(content, status, date, userId);
	}

	public void delete() {
		this.deletedAt = LocalDateTime.now();
	}

	public boolean isDeleted() {
		return deletedAt != null;
	}

	public void updateContentAndStatus(String content, DiaryVisibility status) {
		if (isDeleted()) {
			throw new IllegalStateException("삭제된 일기는 수정할 수 없습니다.");
		}
		validateContent(content);
		validateRequired(status, "공개범위는 필수입니다.");
		this.content = content;
		this.status = status;
	}

	public boolean isOwner(String userId) {
		return this.userId != null && userId != null && !userId.isBlank()
				&& Objects.equals(this.userId, userId);
	}

	private static void validateContent(String content) {
		if (content == null || content.isBlank()) {
			throw new IllegalArgumentException("일기 내용은 비어 있을 수 없습니다.");
		}
		if (content.length() > MAX_CONTENT_LENGTH) {
			throw new IllegalArgumentException("일기 내용은 최대 500자까지 입력할 수 있습니다.");
		}
	}

	private static void validateRequired(Object value, String message) {
		if (value == null) {
			throw new IllegalArgumentException(message);
		}
	}
}
