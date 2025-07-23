package store.piku.back.character.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import store.piku.back.character.dto.CharacterResponseDTO;
import store.piku.back.character.service.CharacterService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Character", description = "캐릭터 관련 API")
@Slf4j
@RestController
@RequestMapping("/api/characters")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;

    // 고정 캐릭터 목록 조회 API
    @Operation(summary = "고정 캐릭터 목록 조회", description = "기본으로 제공되는 고정 캐릭터 목록을 조회합니다.")
    @GetMapping("/fixed")
    public ResponseEntity<List<CharacterResponseDTO>> getFixedCharacters() {
        List<CharacterResponseDTO> fixedCharacters = characterService.getFixedCharacters().stream()
                .map(CharacterResponseDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(fixedCharacters);
    }

    // 고정 캐릭터 이미지 조회 API
    @Operation(summary = "고정 캐릭터 이미지 조회", description = "고정 캐릭터의 이미지를 조회합니다.")
    @GetMapping("/fixed/{fileName:.+}")
    public ResponseEntity<Resource> getFixedCharacterImage(@Parameter(description = "이미지 파일명", example = "base_image_1.png") @PathVariable String fileName) {
        try {
            Resource resource = characterService.getFixedCharacterImageAsResource(fileName);

            String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE; // 기본값
            String resourceFilename = resource.getFilename();
            if (resourceFilename != null) {
                if (resourceFilename.endsWith(".png")) contentType = MediaType.IMAGE_PNG_VALUE;
                else if (resourceFilename.endsWith(".jpg") || resourceFilename.endsWith(".jpeg")) contentType = MediaType.IMAGE_JPEG_VALUE;
                else if (resourceFilename.endsWith(".gif")) contentType = MediaType.IMAGE_GIF_VALUE;
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
            
        } catch (RuntimeException e) {
            log.warn("고정 캐릭터 이미지 '{}' 로드 중 오류 발생(서비스 호출): {}", fileName, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
} 