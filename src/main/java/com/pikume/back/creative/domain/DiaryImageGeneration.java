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

	public DiaryImageGeneration(String userId, String prompt, String filePath) {
		this.userId = userId;
		this.prompt = prompt;
		this.filePath = filePath;
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
		this.filePath = newFilePath;
	}

	public void discard() {
		this.deletedAt = LocalDateTime.now();
	}

	public boolean isDiscarded() {
		return deletedAt != null;
	}
}
