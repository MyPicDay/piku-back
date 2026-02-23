package store.piku.back.diary.adapter.in.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import store.piku.back.diary.adapter.in.web.dto.CalendarDiaryResponseDTO;
import store.piku.back.diary.adapter.in.web.dto.DiaryDTO;
import store.piku.back.diary.adapter.in.web.dto.ResponseDiaryDTO;
import store.piku.back.diary.application.port.in.CreateDiaryUseCase;
import store.piku.back.diary.application.port.in.DeleteDiaryUseCase;
import store.piku.back.diary.application.port.in.GetCalendarUseCase;
import store.piku.back.file.FileUtil;
import store.piku.back.global.config.CustomUserDetails;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.global.util.RequestMetaMapper;

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
	private final FileUtil fileUtil;
	private final RequestMetaMapper requestMetaMapper;
	private final Validator validator;

	@Operation(summary = "일기 생성", description = "일기 내용과 사진을 받아 새로운 일기를 생성합니다. `multipart/form-data` 형식으로 요청해야 합니다.")
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ResponseDiaryDTO> createDiary(
			@Parameter(description = "일기 데이터 (JSON 형식)", schema = @Schema(implementation = DiaryDTO.class)) @RequestPart("diary") DiaryDTO diary,
			@RequestPart(value = "photos", required = false) List<MultipartFile> photos,
			@AuthenticationPrincipal CustomUserDetails userDetails,
			HttpServletRequest request) {
		log.info("{}님 일기와 사진 {}개 등록 요청", userDetails.getId(), photos == null ? 0 : photos.size());
		try {
			Set<ConstraintViolation<DiaryDTO>> violations = validator.validate(diary);
			if (!violations.isEmpty()) {
				throw new ConstraintViolationException(violations);
			}

			RequestMetaInfo requestMetaInfo = requestMetaMapper.extractMetaInfo(request);
			ResponseDiaryDTO isSaved = createDiaryUseCase.createDiary(diary, photos, userDetails.getId(), requestMetaInfo);
			if (isSaved != null) {
				return ResponseEntity.status(HttpStatus.CREATED).body(isSaved);
			}
			return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
		} catch (JsonProcessingException e) {
			log.error("JSON parsing error for diary data: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		} catch (IllegalArgumentException e) {
			log.error("일기 생성 중 오류 발생: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
		} catch (IOException e) {
			log.error("IOException 발생: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}

	@Operation(summary = "일기 이미지 조회", description = "일기에 첨부된 이미지를 조회합니다.")
	@GetMapping("/images/{userId}/{filename:.+}")
	public ResponseEntity<Resource> getFile(@Parameter(description = "사용자 ID") @PathVariable String userId,
			@Parameter(description = "이미지 파일명") @PathVariable String filename) {
		log.info("이미지 파일 요청 - userId: {}, filename: {}", userId, filename);
		try {
			Resource resource = fileUtil.loadFileAsResource(userId + "/" + filename);
			String contentType = fileUtil.getContentType(filename);

			return ResponseEntity.ok()
					.contentType(MediaType.parseMediaType(contentType))
					.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
					.cacheControl(org.springframework.http.CacheControl
							.maxAge(1, java.util.concurrent.TimeUnit.DAYS)
							.cachePublic()
							.immutable())
					.body(resource);

		} catch (Exception e) {
			log.error("이미지 파일 로드 실패 - userId: {}, filename: {}, error: {}", userId, filename, e.getMessage(), e);
			return ResponseEntity.notFound().build();
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
			HttpServletRequest request) {
		RequestMetaInfo requestMetaInfo = requestMetaMapper.extractMetaInfo(request);
		List<CalendarDiaryResponseDTO> diaries = getCalendarUseCase.findMonthlyDiaries(userId, year, month,
				requestMetaInfo);

		return ResponseEntity.ok()
				.cacheControl(org.springframework.http.CacheControl
						.noCache()
						.mustRevalidate())
				.body(diaries);
	}
}
