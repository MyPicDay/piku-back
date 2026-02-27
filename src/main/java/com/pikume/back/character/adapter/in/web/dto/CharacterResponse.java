package com.pikume.back.character.adapter.in.web.dto;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import com.pikume.back.character.domain.Character;
import com.pikume.back.character.domain.vo.CharacterCreationType;
import com.pikume.back.global.util.FileConstants;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * 캐릭터 API 응답 DTO
 */
@Slf4j
@Getter
public class CharacterResponse {

	private final Long id;
	private final String displayImageUrl;
	private final CharacterCreationType type;

	public CharacterResponse(Long id, String displayImageUrl, CharacterCreationType type) {
		this.id = id;
		this.displayImageUrl = displayImageUrl;
		this.type = type;
	}

	public static CharacterResponse fromEntity(Character character) {
		if (character == null)
			return null;

		String characterImageBaseApiUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
				.pathSegment("api", "characters")
				.toUriString();

		String finalImageUrl = null;
		if (character.getImageUrl() != null && !character.getImageUrl().isEmpty()) {
			if (character.getType() == CharacterCreationType.FIXED) {
				finalImageUrl = characterImageBaseApiUrl + "/" + FileConstants.FIXED_CHARACTER_SUB_DIR_NAME + "/"
						+ character.getImageUrl();
			} else if (character.getType() == CharacterCreationType.AI_GENERATED) {
				String userId = character.getUserId();
				if (userId != null && !userId.isBlank()) {
					finalImageUrl = characterImageBaseApiUrl + "/" + userId + "/" + character.getImageUrl();
				} else {
					log.warn("AI 생성 캐릭터의 사용자 정보가 없어 이미지 URL을 완전하게 생성할 수 없습니다: 캐릭터 ID = {}", character.getId());
				}
			}
		}

		return new CharacterResponse(
				character.getId(),
				finalImageUrl,
				character.getType());
	}
}
