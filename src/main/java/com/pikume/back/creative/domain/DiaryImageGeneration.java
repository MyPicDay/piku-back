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

	public void saveDiaryId(Long diaryId) {
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
