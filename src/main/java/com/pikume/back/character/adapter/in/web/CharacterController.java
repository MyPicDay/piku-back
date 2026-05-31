package com.pikume.back.character.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.pikume.back.character.adapter.in.web.dto.CharacterResponse;
import com.pikume.back.character.application.port.in.GetCharacterUseCase;

import java.util.List;

/**
 * Character Web Adapter (Inbound)
 * UseCase 인터페이스를 통해 Application Service에 접근합니다.
 */
@Tag(name = "Character", description = "캐릭터 관련 API")
@Slf4j
@RestController
@RequestMapping("/api/characters")
@RequiredArgsConstructor
public class CharacterController {

	private final GetCharacterUseCase getCharacterUseCase;

	@Operation(summary = "고정 캐릭터 목록 조회", description = "기본으로 제공되는 고정 캐릭터 목록을 조회합니다.")
	@GetMapping("/fixed")
	public ResponseEntity<List<CharacterResponse>> getFixedCharacters() {
		List<CharacterResponse> fixedCharacters = getCharacterUseCase.getFixedCharacters().stream()
				.map(CharacterResponse::fromResult)
				.toList();
		return ResponseEntity.ok(fixedCharacters);
	}
}
