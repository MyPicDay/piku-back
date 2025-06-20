package store.piku.back.diary.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import store.piku.back.diary.dto.CalendarDiaryResponseDTO;
import store.piku.back.diary.dto.DiaryDTO;
import store.piku.back.diary.dto.ResponseDTO;
import store.piku.back.diary.dto.ResponseDiaryDTO;
import store.piku.back.diary.service.DiaryService;
import store.piku.back.file.FileUtil;
import store.piku.back.global.config.CustomUserDetails;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.global.util.RequestMetaMapper;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@Tag(name = "Diary", description = "일기 관련 API")
@RestController
@Slf4j
@RequestMapping("/api/diary")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryservice;
    private final FileUtil fileUtil;
    private final RequestMetaMapper requestMetaMapper;

    @Operation(summary = "일기 생성", description = "일기 내용과 사진을 받아 새로운 일기를 생성합니다. `multipart/form-data` 형식으로 요청해야 합니다.")
    @SecurityRequirement(name = "JWT")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDiaryDTO> createDiary(@ModelAttribute DiaryDTO diaryDTO, @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("{}님 일기와 {} 등록 요청", userDetails.getId(), diaryDTO.getPhotos());
        ResponseDiaryDTO isSaved = diaryservice.createDiary(diaryDTO, userDetails.getId());

        if (isSaved != null) {
            log.info("{}님 일기,사진 등록 성공", userDetails.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(isSaved);
        } else {
            log.error("{}님 일기, 사진 등록 실패", userDetails.getId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "일기 상세 조회", description = "특정 일기의 상세 정보를 조회합니다.")
    @SecurityRequirement(name = "JWT")
    @GetMapping("/{diaryId}")
    public ResponseEntity<?> getDiaryWithPhotos(@PathVariable Long diaryId , HttpServletRequest request) {
        try {
            log.info("Diary 조회 요청 - diaryId: {}", diaryId);

            ResponseDTO response = diaryservice.getDiaryWithPhotos(diaryId, request);
            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            // 엔티티(일기 또는 사진) 못 찾았을 때 404 반환
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (AccessDeniedException e) {
            // 접근 권한 없을 때 403 반환
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            // 기타 서버 에러 500 반환
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러가 발생했습니다.");
        }
    }


    // 이미지 파일 직접 스트림으로 반환하는 API 추가 ( 재요청 )
    @Operation(summary = "일기 이미지 조회", description = "일기에 첨부된 이미지를 조회합니다.")
    @GetMapping("/images/{userId}/{filename:.+}")
    public ResponseEntity<Resource> getFile(@Parameter(description = "사용자 ID") @PathVariable String userId, @Parameter(description = "이미지 파일명") @PathVariable String filename) {
        log.info("이미지 파일 요청 - userId: {}, filename: {}", userId, filename);
        try {
            Resource resource = fileUtil.loadFileAsResource(userId + "/" + filename);
            String contentType = fileUtil.getContentType(filename);

            log.info("이미지 파일 로드 성공 - 경로: {}/{}", userId, filename);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("이미지 파일 로드 실패 - userId: {}, filename: {}, error: {}", userId, filename, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "월별 일기 목록 조회", description = "특정 사용자의 월별 일기 목록을 조회합니다. (캘린더용)")
    @SecurityRequirement(name = "JWT")
    @Parameters({
            @Parameter(name = "userId", description = "사용자 ID", required = true),
            @Parameter(name = "year", description = "조회할 연도", required = true),
            @Parameter(name = "month", description = "조회할 월", required = true)
    })
    @GetMapping("/user/{userId}/monthly")
    public ResponseEntity<List<CalendarDiaryResponseDTO>> getMonthlyDiaries(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable String userId,
            @RequestParam int year,
            @RequestParam int month,
            HttpServletRequest request
    ) {
        // 해당하는 연과 월에 맞는 다이어리 정보들을 반환합니다
        // TODO: userId가 현재 로그인한 사용자와 다를 경우 예외 처리 추가
        RequestMetaInfo requestMetaInfo = requestMetaMapper.extractMetaInfo(request);
        List<CalendarDiaryResponseDTO> diaries = diaryservice.findMonthlyDiaries(userId, year, month, requestMetaInfo);
        return ResponseEntity.ok(diaries);
    }
}
