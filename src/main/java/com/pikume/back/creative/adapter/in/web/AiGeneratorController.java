package com.pikume.back.creative.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.pikume.back.creative.adapter.in.web.dto.AiDiaryResponse;
import com.pikume.back.creative.application.dto.GeneratedImageResult;
import com.pikume.back.creative.application.port.in.GenerateImageUseCase;
import com.pikume.back.global.config.CustomUserDetails;
import com.pikume.back.global.dto.RequestMetaInfo;
import com.pikume.back.global.service.RedisService;
import com.pikume.back.global.util.RequestMetaMapper;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "AI", description = "AI 관련 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class AiGeneratorController {

	private final RedisService redisService;
	private static final int MAX_AI_REQUESTS_PER_DAY = 3;
	private static final String AI_GENERATE_ACTION = "ai_generate";

	private final GenerateImageUseCase generateImageUseCase;
	private final RequestMetaMapper requestMetaMapper;

	@Operation(summary = "AI 일기 이미지 생성", description = "일기 내용을 기반으로 AI 이미지를 생성합니다.")
	@SecurityRequirement(name = "JWT")
	@PostMapping("/diary/ai/generate")
	public ResponseEntity<Object> generateDiaryImage(
			@RequestBody Map<String, String> body,
			HttpServletRequest request,
			@AuthenticationPrincipal CustomUserDetails customUserDetails) {

		String content = body.get("content");
		String userId = customUserDetails.getId();

		if (redisService.isLimitExceeded(AI_GENERATE_ACTION, userId, MAX_AI_REQUESTS_PER_DAY)) {
			return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
					.body("일일 생성 횟수(" + MAX_AI_REQUESTS_PER_DAY + "회)를 모두 사용하셨습니다.");
		}

		RequestMetaInfo requestMetaInfo = requestMetaMapper.extractMetaInfo(request);

		try {
			GeneratedImageResult generation = generateImageUseCase.generateDiaryImage(content, userId, requestMetaInfo);
			log.info("Generated image URL: {}", generation.imageUrl());
			redisService.incrementRequestCount(AI_GENERATE_ACTION, userId);
			return ResponseEntity.ok(new AiDiaryResponse(generation.generationId(), generation.imageUrl(), null));
		} catch (RuntimeException e) {
			log.error("AI 이미지 생성 실패", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new AiDiaryResponse(null, null, e.getMessage()));
		}
	}

	@GetMapping("/diary/ai/generate")
	public ResponseEntity<Map<String, Integer>> getRemainingRequests(
			@AuthenticationPrincipal CustomUserDetails customUserDetails) {

		int remainingCount = redisService.getRemainingCount(
				AI_GENERATE_ACTION, customUserDetails.getId(), MAX_AI_REQUESTS_PER_DAY);

		Map<String, Integer> response = new HashMap<>();
		response.put("remainingRequests", remainingCount);

		return ResponseEntity.ok(response);
	}
}
