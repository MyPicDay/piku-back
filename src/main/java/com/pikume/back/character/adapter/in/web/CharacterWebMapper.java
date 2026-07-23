package com.pikume.back.character.adapter.in.web;

import com.pikume.back.character.adapter.in.web.dto.CharacterResponse;
import com.pikume.back.character.application.dto.CharacterResult;
import com.pikume.back.global.port.out.ResolveObjectUrlPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CharacterWebMapper {

	private final ResolveObjectUrlPort resolveObjectUrlPort;

	public CharacterResponse toResponse(CharacterResult character) {
		String imageReference = character.imageReference();
		String displayImageUrl = isAbsoluteUrl(imageReference)
				? imageReference
				: resolveObjectUrlPort.resolveObjectUrl(imageReference, true);
		return new CharacterResponse(character.id(), displayImageUrl, character.type());
	}

	private boolean isAbsoluteUrl(String value) {
		return value != null && (value.startsWith("http://") || value.startsWith("https://"));
	}
}
