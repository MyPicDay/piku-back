package store.piku.back.diary.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import store.piku.back.diary.dto.CalendarDiaryResponseDTO;
import store.piku.back.diary.dto.DiaryDTO;
import store.piku.back.diary.dto.ResponseDTO;
import store.piku.back.diary.service.DiaryService;
import store.piku.back.file.FileUtil;
import store.piku.back.global.config.CustomUserDetails;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.global.util.RequestMetaMapper;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/diary")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryservice;
    private final FileUtil fileUtil;
    private final RequestMetaMapper requestMetaMapper;

    @PostMapping
    public ResponseEntity<String> createDiary(@ModelAttribute DiaryDTO diaryDTO, @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("{}님 일기와 {} 등록 요청", userDetails.getId(), diaryDTO.getPhotos());
        boolean isSaved = diaryservice.createDiary(diaryDTO, userDetails.getId());

        if (isSaved) {
            log.info("{}님 일기,사진 등록 성공", userDetails.getId());
            return ResponseEntity.ok("사진이 성공적으로 저장되었습니다.");
        } else {
            log.error("{}님 일기, 사진 등록 실패", userDetails.getId());
            return ResponseEntity.internalServerError().body("사진 저장에 실패했습니다.");
        }
    }

    @GetMapping("/{diaryId}")
    public ResponseEntity<ResponseDTO> getDiaryWithPhotos(@PathVariable Long diaryId) {
        log.info("Diary 조회 요청 - diaryId: {}", diaryId);
        ResponseDTO response = diaryservice.getDiaryWithPhotos(diaryId);
        log.info("Diary 조회 완료 - diaryId: {}", diaryId);
        return ResponseEntity.ok(response);
    }


    // 이미지 파일 직접 스트림으로 반환하는 API 추가 ( 재요청 )
    @GetMapping("/images/{userId}/{filename:.+}")
    public ResponseEntity<Resource> getFile(@PathVariable String userId, @PathVariable String filename) {
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
