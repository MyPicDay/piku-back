package com.pikume.back.character.adapter.in.web.dto;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import com.pikume.back.character.application.dto.CharacterResult;
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

	public static CharacterResponse fromResult(CharacterResult character) {
		if (character == null)
			return null;

		String characterImageBaseApiUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
				.pathSegment("api", "characters")
				.toUriString();

		String finalImageUrl = null;
		if (character.imageUrl() != null && !character.imageUrl().isEmpty()) {
			if (character.type() == CharacterCreationType.FIXED) {
				finalImageUrl = characterImageBaseApiUrl + "/" + FileConstants.FIXED_CHARACTER_SUB_DIR_NAME + "/"
						+ character.imageUrl();
			} else if (character.type() == CharacterCreationType.AI_GENERATED) {
				String userId = character.userId();
				if (userId != null && !userId.isBlank()) {
					finalImageUrl = characterImageBaseApiUrl + "/" + userId + "/" + character.imageUrl();
				} else {
					log.warn("AI 생성 캐릭터의 사용자 정보가 없어 이미지 URL을 완전하게 생성할 수 없습니다: 캐릭터 ID = {}", character.id());
				}
			}
		}

		return new CharacterResponse(
				character.id(),
				finalImageUrl,
				character.type());
	}
}
