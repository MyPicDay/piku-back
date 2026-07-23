package com.pikume.back.character.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.pikume.back.character.domain.vo.CharacterCreationType;
import com.pikume.back.character.domain.vo.CharacterImageReference;
import com.pikume.back.global.entity.BaseEntity;

/**
 * Character Aggregate Root
 * 캐릭터 이미지와 생성 유형을 관리합니다.
 * User와의 관계는 ID 참조(String userId)로 유지합니다.
 */
@Getter
@Entity
@Table(name = "characters")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Character extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", length = 36)
	private String userId;

	@Column(name = "image_url", nullable = false)
	private String imageReference;

	@Enumerated(EnumType.STRING)
	private CharacterCreationType type;

	private Character(String userId, CharacterImageReference imageReference, CharacterCreationType type) {
		this.userId = userId;
		this.imageReference = imageReference.value();
		this.type = type;
	}

	public static Character fixed(String imageReference) {
		return new Character(null, CharacterImageReference.of(imageReference), CharacterCreationType.FIXED);
	}

	public static Character aiGenerated(String userId, String imageReference) {
		if (userId == null || userId.isBlank()) {
			throw new IllegalArgumentException("AI 생성 캐릭터에는 사용자 식별자가 필요합니다.");
		}
		return new Character(userId.trim(), CharacterImageReference.of(imageReference), CharacterCreationType.AI_GENERATED);
	}
}
