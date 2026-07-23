package com.pikume.back.character.application.dto;

import com.pikume.back.character.domain.vo.CharacterCreationType;

public record CharacterResult(Long id, String userId, String imageReference, CharacterCreationType type) {
}
