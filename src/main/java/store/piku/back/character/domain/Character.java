package store.piku.back.character.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import store.piku.back.character.domain.vo.CharacterCreationType;
import store.piku.back.global.entity.BaseEntity;

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

	@Column(name = "user_id")
	private String userId;

	private String imageUrl;

	@Enumerated(EnumType.STRING)
	private CharacterCreationType type;

	/**
	 * AI 생성 캐릭터 생성자
	 */
	public Character(String userId, String imageUrl) {
		this.userId = userId;
		this.imageUrl = imageUrl;
		this.type = CharacterCreationType.AI_GENERATED;
	}

	/**
	 * 고정 캐릭터 생성자
	 */
	public Character(String imageUrl, CharacterCreationType type) {
		this.imageUrl = imageUrl;
		this.type = type;
	}
}
