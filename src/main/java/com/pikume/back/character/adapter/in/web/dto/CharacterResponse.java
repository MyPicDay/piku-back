package com.pikume.back.character.adapter.in.web.dto;

import lombok.Getter;
import com.pikume.back.character.domain.vo.CharacterCreationType;

/**
 * 캐릭터 API 응답 DTO
 */
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

}
