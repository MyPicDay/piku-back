package com.pikume.back.creative.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.pikume.back.global.entity.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "diary_image_generation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class DiaryImageGeneration extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String userId;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String prompt;

	private String filePath;

	private Long diaryId;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	private DiaryImageGeneration(String userId, String prompt, String filePath) {
		this.userId = requireText(userId, "생성 사용자 식별자는 필수입니다.");
		this.prompt = requireText(prompt, "생성 프롬프트는 필수입니다.");
		this.filePath = requireText(filePath, "생성 이미지 참조는 필수입니다.");
	}

	public static DiaryImageGeneration create(String userId, String prompt, String filePath) {
		return new DiaryImageGeneration(userId, prompt, filePath);
	}

	public void attachToDiary(Long diaryId) {
		if (diaryId == null || diaryId <= 0) {
			throw new IllegalArgumentException("일기 ID는 양수여야 합니다.");
		}
		if (isDiscarded()) {
			throw new IllegalStateException("폐기된 생성 이미지는 일기에 연결할 수 없습니다.");
		}
		if (this.diaryId != null) {
			throw new IllegalStateException("이미 다른 일기에 연결된 생성 이미지입니다.");
		}
		this.diaryId = diaryId;
	}

	public void updateFilePath(String newFilePath) {
		this.filePath = requireText(newFilePath, "생성 이미지 참조는 필수입니다.");
	}

	public void discard() {
		this.deletedAt = LocalDateTime.now();
	}

	public boolean isDiscarded() {
		return deletedAt != null;
	}

	private static String requireText(String value, String message) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(message);
		}
		return value;
	}
}
