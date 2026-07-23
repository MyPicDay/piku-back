package com.pikume.back.creative.adapter.in.web;

import com.pikume.back.creative.adapter.in.web.dto.AiDiaryResponse;
import com.pikume.back.creative.adapter.in.web.dto.AiGenerationQuotaResponse;
import com.pikume.back.creative.adapter.in.web.dto.GenerateDiaryImageRequest;
import com.pikume.back.creative.application.dto.GeneratedImageResult;
import com.pikume.back.creative.application.port.in.GenerateImageUseCase;
import com.pikume.back.creative.application.port.in.QueryAiGenerationQuotaUseCase;
import com.pikume.back.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI", description = "AI 관련 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AiGeneratorController {

	private final QueryAiGenerationQuotaUseCase queryAiGenerationQuotaUseCase;
	private final GenerateImageUseCase generateImageUseCase;

	@Operation(summary = "AI 일기 이미지 생성", description = "일기 내용을 기반으로 AI 이미지를 생성합니다.")
	@SecurityRequirement(name = "JWT")
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "AI 이미지 생성 성공",
					content = @Content(schema = @Schema(implementation = AiDiaryResponse.class))),
			@ApiResponse(
					responseCode = "400",
					description = "유효하지 않은 요청",
					content = @Content(
							mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
							schema = @Schema(implementation = ProblemDetail.class))),
			@ApiResponse(
					responseCode = "429",
					description = "일일 생성 한도 초과",
					content = @Content(
							mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
							schema = @Schema(implementation = ProblemDetail.class))),
			@ApiResponse(
					responseCode = "500",
					description = "이미지 생성 실패",
					content = @Content(
							mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
							schema = @Schema(implementation = ProblemDetail.class)))
	})
	@PostMapping("/diary/ai/generate")
	public ResponseEntity<AiDiaryResponse> generateDiaryImage(
			@Valid @RequestBody GenerateDiaryImageRequest request,
			@AuthenticationPrincipal UserPrincipal userPrincipal) {

		String userId = userPrincipal.getId();
		GeneratedImageResult generation = generateImageUseCase.generateDiaryImage(request.content(), userId);
		return ResponseEntity.ok(new AiDiaryResponse(generation.generationId(), generation.imageUrl(), null));
	}

	@Operation(summary = "AI 일기 이미지 잔여 생성 횟수 조회")
	@SecurityRequirement(name = "JWT")
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "잔여 생성 횟수",
					content = @Content(schema = @Schema(implementation = AiGenerationQuotaResponse.class))),
			@ApiResponse(
					responseCode = "500",
					description = "잔여 생성 횟수 조회 실패",
					content = @Content(
							mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
							schema = @Schema(implementation = ProblemDetail.class)))
	})
	@GetMapping("/diary/ai/generate")
	public ResponseEntity<AiGenerationQuotaResponse> getRemainingRequests(
			@AuthenticationPrincipal UserPrincipal userPrincipal) {

		int remainingCount = queryAiGenerationQuotaUseCase.getRemainingGenerationCount(userPrincipal.getId());
		return ResponseEntity.ok(new AiGenerationQuotaResponse(remainingCount));
	}
}
