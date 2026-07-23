package com.pikume.back.character.adapter.in.web;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.pikume.back.character.adapter.in.web.dto.CharacterResponse;
import com.pikume.back.character.application.port.in.QueryFixedCharacterCatalogUseCase;

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

	private final QueryFixedCharacterCatalogUseCase queryFixedCharacterCatalogUseCase;
	private final CharacterWebMapper characterWebMapper;

	@Operation(summary = "고정 캐릭터 목록 조회", description = "기본으로 제공되는 고정 캐릭터 목록을 조회합니다.")
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "고정 캐릭터 목록",
					content = @Content(array = @ArraySchema(schema = @Schema(implementation = CharacterResponse.class)))),
			@ApiResponse(
					responseCode = "500",
					description = "캐릭터 목록 조회 실패",
					content = @Content(
							mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
							schema = @Schema(implementation = ProblemDetail.class)))
	})
	@GetMapping("/fixed")
	public ResponseEntity<List<CharacterResponse>> getFixedCharacters() {
		List<CharacterResponse> fixedCharacters = queryFixedCharacterCatalogUseCase.queryFixedCharacters().stream()
				.map(characterWebMapper::toResponse)
				.toList();
		return ResponseEntity.ok(fixedCharacters);
	}
}
