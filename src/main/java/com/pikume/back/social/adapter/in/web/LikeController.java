package com.pikume.back.social.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.pikume.back.global.config.CustomUserDetails;
import com.pikume.back.global.dto.RequestMetaInfo;
import com.pikume.back.global.util.RequestMetaMapper;
import com.pikume.back.social.adapter.in.web.dto.LikeResponse;
import com.pikume.back.social.application.dto.LikeResult;
import com.pikume.back.social.application.port.in.LikeUseCase;

@Tag(name = "Like", description = "좋아요 API")
@RestController
@RequestMapping("/api/likes")
@RequiredArgsConstructor
@Slf4j
public class LikeController {

	private final LikeUseCase likeUseCase;
	private final RequestMetaMapper requestMetaMapper;

	@Operation(summary = "좋아요 추가", description = "일기에 좋아요를 추가합니다.")
	@PostMapping("/diary/{diaryId}")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<LikeResponse> addLike(
			@Parameter(description = "일기 ID", required = true) @PathVariable Long diaryId,
			@AuthenticationPrincipal CustomUserDetails userDetails,
			HttpServletRequest request) {
		RequestMetaInfo requestMetaInfo = requestMetaMapper.extractMetaInfo(request);
		LikeResult response = likeUseCase.addLike(userDetails.getId(), diaryId, requestMetaInfo);
		return ResponseEntity.ok(toResponse(response));
	}

	@Operation(summary = "좋아요 취소", description = "일기의 좋아요를 취소합니다.")
	@DeleteMapping("/diary/{diaryId}")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<LikeResponse> removeLike(
			@Parameter(description = "일기 ID", required = true) @PathVariable Long diaryId,
			@AuthenticationPrincipal CustomUserDetails userDetails) {
		LikeResult response = likeUseCase.removeLike(userDetails.getId(), diaryId);
		return ResponseEntity.ok(toResponse(response));
	}

	/*
	@Operation(summary = "좋아요 상태 조회", description = "일기의 좋아요 수와 현재 사용자의 좋아요 여부를 조회합니다.")
	@GetMapping("/diary/{diaryId}")
	public ResponseEntity<LikeResponse> getLikeStatus(
			@Parameter(description = "일기 ID", required = true) @PathVariable Long diaryId,
			@AuthenticationPrincipal CustomUserDetails userDetails) {
		String userId = userDetails != null ? userDetails.getId() : null;
		LikeResult response = likeUseCase.getLikeStatus(userId, diaryId);
		return ResponseEntity.ok(toResponse(response));
	}

	@Operation(summary = "좋아요 수 조회", description = "일기의 좋아요 수만 조회합니다.")
	@GetMapping("/diary/{diaryId}/count")
	public ResponseEntity<Long> getLikeCount(
			@Parameter(description = "일기 ID", required = true) @PathVariable Long diaryId,
			@AuthenticationPrincipal CustomUserDetails userDetails) {
		String userId = userDetails != null ? userDetails.getId() : null;
		long count = likeUseCase.getLikeCount(userId, diaryId);
		return ResponseEntity.ok(count);
	}
	*/

	private LikeResponse toResponse(LikeResult result) {
		return LikeResponse.builder()
				.diaryId(result.diaryId())
				.likeCount(result.likeCount())
				.isLiked(result.liked())
				.build();
	}
}
