package com.pikume.back.diary.adapter.in.web;

import com.pikume.back.diary.adapter.in.web.dto.*;
import com.pikume.back.diary.application.dto.*;
import com.pikume.back.diary.application.exception.DiaryInvalidRequestException;
import com.pikume.back.diary.application.port.in.*;
import com.pikume.back.global.config.CustomUserDetails;
import com.pikume.back.global.dto.UploadedFileData;
import com.pikume.back.global.error.CommonProblemType;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.global.error.ValidationProblemType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Tag(name = "Diary", description = "일기 관련 API")
@RestController
@Slf4j
@RequestMapping("/api/diary")
@RequiredArgsConstructor
public class DiaryController {

	private final CreateDiaryUseCase createDiaryUseCase;
	private final DeleteDiaryUseCase deleteDiaryUseCase;
	private final GetCalendarUseCase getCalendarUseCase;
	private final UpdateDiaryUseCase updateDiaryUseCase;
	private final GetDiaryGalleryUseCase getDiaryGalleryUseCase;
	private final Validator validator;
	private final ProblemDetailFactory problemDetailFactory;

	@Operation(summary = "일기 생성", description = "일기 내용과 사진을 받아 새로운 일기를 생성합니다. `multipart/form-data` 형식으로 요청해야 합니다.")
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> createDiary(
			@Parameter(description = "일기 데이터 (JSON 형식)", schema = @Schema(implementation = DiaryDTO.class)) @RequestPart("diary") DiaryDTO diary,
			@RequestPart(value = "photos", required = false) List<MultipartFile> photos,
			@AuthenticationPrincipal CustomUserDetails userDetails) {
		log.info("{}님 일기와 사진 {}개 등록 요청", userDetails.getId(), photos == null ? 0 : photos.size());
		try {
			Set<ConstraintViolation<DiaryDTO>> violations = validator.validate(diary);
			if (!violations.isEmpty()) {
				throw new ConstraintViolationException(violations);
			}

			DiaryCreatedResult isSaved = createDiaryUseCase.createDiary(
					toCreateDiaryCommand(diary),
					toUploadedFiles(photos),
					userDetails.getId());
			if (isSaved != null) {
				return ResponseEntity.status(HttpStatus.CREATED).body(new ResponseDiaryDTO(isSaved.diaryId(), isSaved.content()));
			}
			return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
					.body(problemDetailFactory.create(
							CommonProblemType.UNPROCESSABLE_CONTENT,
							"일기를 저장할 수 없습니다.",
							"/api/diary"));
		} catch (DiaryInvalidRequestException | IllegalArgumentException e) {
			log.error("일기 생성 중 오류 발생: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(problemDetailFactory.create(
							ValidationProblemType.INVALID_REQUEST,
							e.getMessage(),
							"/api/diary"));
		} catch (IOException e) {
			log.error("IOException 발생: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(problemDetailFactory.create(
							CommonProblemType.INTERNAL_SERVER_ERROR,
							"일기 저장 중 오류가 발생했습니다.",
							"/api/diary"));
		}
	}

	@Operation(summary = "일기 삭제", description = "일기를 soft delete 방식으로 삭제합니다. 본인의 일기만 삭제할 수 있습니다.")
	@DeleteMapping("/{diaryId}")
	public ResponseEntity<Void> deleteDiary(
			@Parameter(description = "일기 ID") @PathVariable Long diaryId,
			@AuthenticationPrincipal CustomUserDetails userDetails) {
		log.info("{}님 일기 ID [{}] 삭제 요청", userDetails.getId(), diaryId);
		deleteDiaryUseCase.deleteDiary(diaryId, userDetails.getId());
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "일기 수정", description = "일기의 내용과 공개범위를 수정합니다. 본인의 일기만 수정할 수 있습니다.")
	@PatchMapping(value = "/{diaryId}", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<UpdateDiaryResponse> updateDiary(
			@Parameter(description = "일기 ID") @PathVariable Long diaryId,
			@Valid @RequestBody UpdateDiaryRequest request,
			@AuthenticationPrincipal CustomUserDetails userDetails) {
		log.info("{}님 일기 ID [{}] 수정 요청", userDetails.getId(), diaryId);
		DiaryUpdatedResult result = updateDiaryUseCase.updateDiary(
				diaryId,
				toUpdateDiaryCommand(request),
				userDetails.getId());
		return ResponseEntity.ok(new UpdateDiaryResponse(result.diaryId(), result.status(), result.content()));
	}

	@Operation(summary = "월별 일기 목록 조회", description = "특정 사용자의 월별 일기 목록을 조회합니다. (캘린더용)")
	@Parameters({
			@Parameter(name = "userId", description = "사용자 ID", required = true),
			@Parameter(name = "year", description = "조회할 연도", required = true),
			@Parameter(name = "month", description = "조회할 월", required = true)
	})
	@GetMapping("/user/{userId}/monthly")
	public ResponseEntity<List<CalendarDiaryResponseDTO>> getMonthlyDiaries(
			@PathVariable String userId,
			@RequestParam int year,
			@RequestParam int month,
			@AuthenticationPrincipal CustomUserDetails userDetails) {
		String viewerId = userDetails != null ? userDetails.getId() : null;
		List<CalendarDiaryResponseDTO> diaries = getCalendarUseCase.findMonthlyDiaries(userId, viewerId, year, month).stream()
				.map(this::toCalendarDiaryResponse)
				.toList();

		return ResponseEntity.ok()
				.cacheControl(org.springframework.http.CacheControl
						.noCache()
				.mustRevalidate())
				.body(diaries);
	}

	@Operation(summary = "사용자 일기 사진 갤러리 조회", description = "특정 사용자가 등록한 일기 대표 사진을 cursor 기반으로 조회합니다.")
	@Parameters({
			@Parameter(name = "userId", description = "사용자 ID", required = true),
			@Parameter(name = "cursor", description = "다음 페이지 조회용 opaque cursor"),
			@Parameter(name = "limit", description = "페이지 크기. 1~10 사이 정수이며 기본값은 10입니다.")
	})
	@GetMapping("/user/{userId}/gallery")
	public ResponseEntity<DiaryGalleryPageResponse<DiaryGalleryItemResponse>> getUserDiaryGallery(
			@PathVariable String userId,
			@RequestParam(required = false) String cursor,
			@RequestParam(defaultValue = "10") @Min(1) @Max(10) int limit,
			@AuthenticationPrincipal CustomUserDetails userDetails) {
		String viewerId = userDetails != null ? userDetails.getId() : null;
		DiaryGalleryPage<DiaryGalleryItemView> page = getDiaryGalleryUseCase.findGallery(
				userId,
				viewerId,
				cursor,
				limit);

		return ResponseEntity.ok(toDiaryGalleryPageResponse(page));
	}

	private CalendarDiaryResponseDTO toCalendarDiaryResponse(CalendarDiaryView diary) {
		return new CalendarDiaryResponseDTO(diary.diaryId(), diary.coverPhotoUrl(), diary.date());
	}

	private DiaryGalleryPageResponse<DiaryGalleryItemResponse> toDiaryGalleryPageResponse(
			DiaryGalleryPage<DiaryGalleryItemView> page) {
		return new DiaryGalleryPageResponse<>(
				page.items().stream()
						.map(this::toDiaryGalleryItemResponse)
						.toList(),
				page.nextCursor(),
				page.hasNext());
	}

	private DiaryGalleryItemResponse toDiaryGalleryItemResponse(DiaryGalleryItemView item) {
		return new DiaryGalleryItemResponse(
				item.diaryId(),
				item.coverPhotoUrl(),
				item.date().toString(),
				item.imageCount(),
				item.status());
	}

	private CreateDiaryCommand toCreateDiaryCommand(DiaryDTO diary) {
		return new CreateDiaryCommand(
				diary.getStatus(),
				diary.getContent(),
				diary.getImageInfos().stream()
						.map(info -> new DiaryImageCommand(info.getType(), info.getOrder(), info.getAiPhotoId(), info.getPhotoIndex()))
						.toList(),
				diary.getDate());
	}

	private UpdateDiaryCommand toUpdateDiaryCommand(UpdateDiaryRequest request) {
		return new UpdateDiaryCommand(request.getStatus(), request.getContent());
	}

	private List<UploadedFileData> toUploadedFiles(List<MultipartFile> photos) throws IOException {
		if (photos == null) {
			return null;
		}
		return photos.stream()
				.map(this::toUploadedFile)
				.toList();
	}

	private UploadedFileData toUploadedFile(MultipartFile file) {
		try {
			return new UploadedFileData(file.getOriginalFilename(), file.getContentType(), file.getBytes());
		} catch (IOException e) {
			throw new IllegalArgumentException("파일을 읽는 중 오류가 발생했습니다.", e);
		}
	}
}
