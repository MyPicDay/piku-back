package store.piku.back.character.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import store.piku.back.character.adapter.in.web.dto.CharacterResponse;
import store.piku.back.character.application.port.in.GetCharacterUseCase;
import store.piku.back.character.application.port.in.ManageCharacterUseCase;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
	private final ManageCharacterUseCase manageCharacterUseCase;

	@Operation(summary = "고정 캐릭터 목록 조회", description = "기본으로 제공되는 고정 캐릭터 목록을 조회합니다.")
	@GetMapping("/fixed")
	public ResponseEntity<List<CharacterResponse>> getFixedCharacters() {
		List<CharacterResponse> fixedCharacters = getCharacterUseCase.getFixedCharacters().stream()
				.map(CharacterResponse::fromEntity)
				.collect(Collectors.toList());
		return ResponseEntity.ok(fixedCharacters);
	}

	@Operation(summary = "고정 캐릭터 이미지 조회", description = "고정 캐릭터의 이미지를 조회합니다.")
	@GetMapping("/fixed/{fileName:.+}")
	public ResponseEntity<Resource> getFixedCharacterImage(
			@Parameter(description = "이미지 파일명", example = "base_image_1.png") @PathVariable String fileName) {
		try {
			Resource resource = manageCharacterUseCase.getFixedCharacterImageAsResource(fileName);

			String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
			String resourceFilename = resource.getFilename();
			if (resourceFilename != null) {
				if (resourceFilename.endsWith(".png"))
					contentType = MediaType.IMAGE_PNG_VALUE;
				else if (resourceFilename.endsWith(".jpg") || resourceFilename.endsWith(".jpeg"))
					contentType = MediaType.IMAGE_JPEG_VALUE;
				else if (resourceFilename.endsWith(".gif"))
					contentType = MediaType.IMAGE_GIF_VALUE;
			}

			return ResponseEntity.ok()
					.contentType(MediaType.parseMediaType(contentType))
					.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
					.cacheControl(CacheControl
							.maxAge(365, TimeUnit.DAYS)
							.cachePublic()
							.immutable())
					.body(resource);

		} catch (RuntimeException e) {
			log.warn("고정 캐릭터 이미지 '{}' 로드 중 오류 발생(서비스 호출): {}", fileName, e.getMessage());
			return ResponseEntity.notFound().build();
		}
	}
}
