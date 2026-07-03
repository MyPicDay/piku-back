package com.pikume.back.creative.adapter.in.web;

import com.pikume.back.creative.adapter.in.web.dto.AiDiaryResponse;
import com.pikume.back.creative.application.dto.GeneratedImageResult;
import com.pikume.back.creative.application.exception.AiGenerationQuotaExceededException;
import com.pikume.back.creative.application.port.in.GenerateImageUseCase;
import com.pikume.back.creative.application.port.in.ManageAiGenerationQuotaUseCase;
import com.pikume.back.global.config.CustomUserDetails;
import com.pikume.back.global.error.CommonProblemType;
import com.pikume.back.global.error.ProblemDetailFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "AI", description = "AI 관련 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class AiGeneratorController {

	private final ManageAiGenerationQuotaUseCase manageAiGenerationQuotaUseCase;

	private final GenerateImageUseCase generateImageUseCase;
	private final ProblemDetailFactory problemDetailFactory;

	@Operation(summary = "AI 일기 이미지 생성", description = "일기 내용을 기반으로 AI 이미지를 생성합니다.")
	@SecurityRequirement(name = "JWT")
	@PostMapping("/diary/ai/generate")
	public ResponseEntity<Object> generateDiaryImage(
			@RequestBody Map<String, String> body,
			@AuthenticationPrincipal CustomUserDetails customUserDetails) {

		String content = body.get("content");
		String userId = customUserDetails.getId();

		try {
			GeneratedImageResult generation = generateImageUseCase.generateDiaryImage(content, userId);
			log.info("Generated image URL: {}", generation.imageUrl());
			return ResponseEntity.ok(new AiDiaryResponse(generation.generationId(), generation.imageUrl(), null));
		} catch (AiGenerationQuotaExceededException e) {
			return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
					.body(problemDetailFactory.create(
							CommonProblemType.RATE_LIMIT_EXCEEDED,
							e.getMessage(),
							"/api/diary/ai/generate"));
		}
	}

	@GetMapping("/diary/ai/generate")
	public ResponseEntity<Map<String, Integer>> getRemainingRequests(
			@AuthenticationPrincipal CustomUserDetails customUserDetails) {

		int remainingCount = manageAiGenerationQuotaUseCase.getRemainingGenerationCount(customUserDetails.getId());

		Map<String, Integer> response = new HashMap<>();
		response.put("remainingRequests", remainingCount);

		return ResponseEntity.ok(response);
	}
}
