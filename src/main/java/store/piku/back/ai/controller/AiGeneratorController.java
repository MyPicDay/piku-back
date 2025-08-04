package store.piku.back.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import store.piku.back.global.service.RedisService;
import store.piku.back.ai.dto.AiDiaryResponseDTO;
import store.piku.back.ai.service.ImageGenerationService;
import store.piku.back.global.config.CustomUserDetails;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.global.util.RequestMetaMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "AI", description = "AI 관련 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class AiGeneratorController {
    private final RedisService redisService;
    private static final int MAX_AI_REQUESTS_PER_DAY = 10;
    private static final String AI_GENERATE_ACTION = "ai_generate";

    private final ImageGenerationService imageGenerationService;
    private final RequestMetaMapper requestMetaMapper;

    @Operation(summary = "AI 일기 이미지 생성", description = "일기 내용을 기반으로 AI 이미지를 생성합니다.")
    @SecurityRequirement(name = "JWT")
    @PostMapping("/diary/ai/generate")
    public ResponseEntity<Object> generateDiaryImage(@RequestBody Map<String, String> body, HttpServletRequest request, @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        String content = body.get("content");
        String userId = customUserDetails.getId();

        // RedisService를 통해 횟수 제한 확인
        if (redisService.isRequestLimitExceeded(AI_GENERATE_ACTION, userId, MAX_AI_REQUESTS_PER_DAY)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("일일 생성 횟수(" + MAX_AI_REQUESTS_PER_DAY + "회)를 모두 사용하셨습니다.");
        }


        RequestMetaInfo requestMetaInfo = requestMetaMapper.extractMetaInfo(request);

        try {
            AiDiaryResponseDTO dto = imageGenerationService.diaryImage(content, userId, requestMetaInfo);
            log.info("Generated image URL: {}", dto.getUrl());
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            log.error("AI 이미지 생성 실패", e);
            AiDiaryResponseDTO errorDto = new AiDiaryResponseDTO(null, null, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDto);
        }
    }
}
