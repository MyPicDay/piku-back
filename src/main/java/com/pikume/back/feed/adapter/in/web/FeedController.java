package com.pikume.back.feed.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.pikume.back.feed.application.dto.FeedCursorPage;
import com.pikume.back.feed.application.dto.FeedCursorRequest;
import com.pikume.back.feed.application.dto.FeedDiaryResult;
import com.pikume.back.feed.application.dto.FeedSortMode;
import com.pikume.back.feed.application.port.in.GetFeedUseCase;
import com.pikume.back.global.config.CustomUserDetails;

@Tag(name = "Feed", description = "피드 관련 API")
@RestController
@Validated
@Slf4j
@RequestMapping("/api/diary")
@RequiredArgsConstructor
public class FeedController {

	private final GetFeedUseCase getFeedUseCase;

	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "일기 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FeedDiaryResult.class))),
			@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
			@ApiResponse(responseCode = "404", description = "대표 사진을 찾을 수 없음", content = @Content),
			@ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
	})
	@Operation(summary = "일기 상세 조회", description = "특정 일기의 상세 정보를 조회합니다.")
	@GetMapping("/{diaryId}")
	public ResponseEntity<FeedDiaryResult> getDiaryWithPhotos(@PathVariable Long diaryId,
			@AuthenticationPrincipal CustomUserDetails customUserDetails) {
		log.info("Diary 조회 요청 - diaryId: {}", diaryId);

		String userId = customUserDetails != null ? customUserDetails.getId() : null;
		FeedDiaryResult response = getFeedUseCase.getDiaryWithPhotos(diaryId, userId);
		if (userId != null) {
			getFeedUseCase.logClick(userId, diaryId);
		}
		return ResponseEntity.ok(response);
	}

	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "일기 조회 성공 ", content = @Content(schema = @Schema(implementation = FeedCursorPage.class))) })
	@Operation(summary = "일기 피드 조회", description = """
			    cursor 기반으로 피드 목록을 조회합니다.
			    - cursor: 다음 페이지 조회용 opaque token
			    - limit: 1~100 사이 정수
			    - sort: recommended(추천순) 또는 latest(기록일 최신순)
			""")
	@GetMapping
	public ResponseEntity<FeedCursorPage<FeedDiaryResult>> getAllDiaries(
			@RequestParam(required = false) String cursor,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
			@Parameter(description = "피드 정렬 모드. 생략 시 recommended(추천순)이며, latest는 기록일 최신순입니다.",
					schema = @Schema(allowableValues = {"recommended", "latest"}, defaultValue = "recommended"))
			@RequestParam(required = false) String sort,
			@AuthenticationPrincipal CustomUserDetails customUserDetails) {
		FeedSortMode sortMode = FeedSortMode.from(sort);
		String userId = customUserDetails != null ? customUserDetails.getId() : null;
		FeedCursorPage<FeedDiaryResult> page = getFeedUseCase.getAllDiaries(
				new FeedCursorRequest(cursor, limit, sortMode),
				userId);
		return ResponseEntity.ok(page);
	}
}
