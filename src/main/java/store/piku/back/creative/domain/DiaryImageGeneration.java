package store.piku.back.creative.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import store.piku.back.global.entity.BaseEntity;

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
}
