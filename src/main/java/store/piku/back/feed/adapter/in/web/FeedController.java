package store.piku.back.feed.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import store.piku.back.diary.adapter.in.web.dto.ResponseDTO;
import store.piku.back.diary.application.service.DiaryQueryService;
import store.piku.back.feed.application.port.in.GetFeedUseCase;
import store.piku.back.global.config.CustomUserDetails;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.global.util.RequestMetaMapper;

import java.util.List;

@Tag(name = "Feed", description = "피드 관련 API")
@RestController
@Slf4j
@RequestMapping("/api/diary")
@RequiredArgsConstructor
public class FeedController {

	private final GetFeedUseCase getFeedUseCase;
	private final DiaryQueryService diaryQueryService;
	private final RequestMetaMapper requestMetaMapper;

	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "일기 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
			@ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
			@ApiResponse(responseCode = "404", description = "대표 사진을 찾을 수 없음", content = @Content),
			@ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
	})
	@Operation(summary = "일기 상세 조회", description = "특정 일기의 상세 정보를 조회합니다.")
	@GetMapping("/{diaryId}")
	public ResponseEntity<ResponseDTO> getDiaryWithPhotos(@PathVariable Long diaryId, HttpServletRequest request,
			@AuthenticationPrincipal CustomUserDetails customUserDetails) {
		log.info("Diary 조회 요청 - diaryId: {}", diaryId);

		RequestMetaInfo requestMetaInfo = requestMetaMapper.extractMetaInfo(request);
		String userId = customUserDetails != null ? customUserDetails.getId() : null;
		ResponseDTO response = getFeedUseCase.getDiaryWithPhotos(diaryId, requestMetaInfo, userId);
		if (userId != null) {
			getFeedUseCase.logClick(userId, diaryId);
		}
		return ResponseEntity.ok(response);
	}

	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "일기 조회 성공 ", content = @Content(schema = @Schema(implementation = ResponseDTO.class))) })
	@Operation(summary = "일기 전체 조회", description = """
			    프론트에서 페이지수, 정렬방법, 페이지 크기 보내줄 수 있습니다.
			    - page: 0 이상 정수
			    - size: 1~100 사이 정수
			    - sort: "createdAt", "userId", "date" 중 하나
			""")
	@GetMapping
	public ResponseEntity<Page<ResponseDTO>> getAllDiaries(
			@ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
			HttpServletRequest request,
			@AuthenticationPrincipal CustomUserDetails customUserDetails) {

		List<String> allowed = List.of("createdAt", "userId", "date");
		Pageable safePageable = diaryQueryService.sanitizePageable(pageable, allowed);

		log.info("safePageable: {}", safePageable);
		RequestMetaInfo requestMetaInfo = requestMetaMapper.extractMetaInfo(request);
		String userId = customUserDetails != null ? customUserDetails.getId() : null;
		Page<ResponseDTO> page = getFeedUseCase.getAllDiaries(safePageable, requestMetaInfo, userId);
		return ResponseEntity.ok(page);
	}
}
