package store.piku.back.diary.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import store.piku.back.user.exception.UserNotFoundException;

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
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDiaryDTO> createDiary(@Valid @ModelAttribute DiaryDTO diaryDTO, @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("{}님 일기와 {} 등록 요청", userDetails.getId(), diaryDTO.getPhotos());
        try {
            ResponseDiaryDTO isSaved = diaryservice.createDiary(diaryDTO, userDetails.getId());
            if (isSaved != null) {
                return ResponseEntity.status(HttpStatus.CREATED).body(isSaved);
            } else {
                log.error("{}님 일기, 사진 등록 실패", userDetails.getId());
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
            }
        }catch(UserNotFoundException e) {
            log.error("유저를 찾을 수 없습니다: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        } catch (IllegalArgumentException e) {
            log.error("일기 생성 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }






    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "일기 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "404", description = "대표 사진을 찾을 수 없음", content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    @Operation(summary = "일기 상세 조회", description = "특정 일기의 상세 정보를 조회합니다.")
    @GetMapping("/{diaryId}")
    public ResponseEntity<ResponseDTO> getDiaryWithPhotos(@PathVariable Long diaryId ,HttpServletRequest request , @AuthenticationPrincipal CustomUserDetails customUserDetails) {

            log.info("Diary 조회 요청 - diaryId: {}", diaryId);

            RequestMetaInfo requestMetaInfo = requestMetaMapper.extractMetaInfo(request);
            ResponseDTO response = diaryservice.getDiaryWithPhotos(diaryId, requestMetaInfo, customUserDetails.getId());
            return ResponseEntity.ok(response);

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




    @ApiResponses(value ={@ApiResponse(responseCode = "200",description = "일기 조회 성공 ", content = @Content(schema = @Schema(implementation = ResponseDTO.class)))})
    @Operation(summary = "일기 전체 조회" ,description = "프론트에서 페이지수,정렬방법,페이지 크기 보내줄 수 있습니다.")
    @GetMapping
    public ResponseEntity<Page<ResponseDTO>> getAllDiaries(
            @ParameterObject
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC, size = 3) Pageable pageable,HttpServletRequest request,@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        try {
            log.info("Pageable: {}", pageable);

            RequestMetaInfo requestMetaInfo = requestMetaMapper.extractMetaInfo(request);
            Page<ResponseDTO> page = diaryservice.getAllDiaries(pageable ,requestMetaInfo,customUserDetails.getId());
            return ResponseEntity.ok(page);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 여기 수정 예정
        }
    }
}
