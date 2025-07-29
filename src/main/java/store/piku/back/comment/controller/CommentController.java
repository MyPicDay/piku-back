package store.piku.back.comment.controller;

import com.google.firebase.messaging.FirebaseMessagingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import store.piku.back.comment.dto.request.CommentRequestDto;
import store.piku.back.comment.dto.request.CommentUpdateDto;
import store.piku.back.comment.dto.response.CommentDeleteResponseDto;
import store.piku.back.comment.dto.response.CommentListResponseDto;
import store.piku.back.comment.dto.response.CommentResponseDto;
import store.piku.back.comment.service.CommentService;
import store.piku.back.global.config.CustomUserDetails;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.global.util.RequestMetaMapper;


@Tag(name = "Comment", description = "댓글 관련 API")
@RestController
@Slf4j
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final RequestMetaMapper requestMetaMapper;

    @Operation(summary = "댓글 작성", description = "댓글을 작성합니다.")
    @SecurityRequirement(name = "JWT")
    @PostMapping
    public ResponseEntity<CommentResponseDto> createComment(@RequestBody CommentRequestDto commentRequestDto, @AuthenticationPrincipal CustomUserDetails userDetails) throws FirebaseMessagingException {
        log.info("사용자 {}님이 {} 일기, {} 댓글에 댓글 등록 요청, 댓글 내용: {}", userDetails.getId(), commentRequestDto.getDiaryId(), commentRequestDto.getParentId(), commentRequestDto.getContent());
        CommentResponseDto isSaved = commentService.createComment(commentRequestDto, userDetails.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(isSaved);
    }

    @Operation(summary = "댓글 수정", description = "댓글 내용을 수정합니다.")
    @PatchMapping("/{commentId}")
    public ResponseEntity<CommentResponseDto> updateComment(@PathVariable Long commentId, @RequestBody CommentUpdateDto updateDto, @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("사용자 {}님이 {} 댓글 수정 요청, 수정할 댓글 내용: {}", userDetails.getId(), commentId, updateDto.getContent());
        CommentResponseDto isSaved = commentService.updateComment(commentId, updateDto, userDetails.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(isSaved);
    }

    @Operation(summary = "원댓글 조회", description = "특정 일기의 루트 댓글을 페이징하여 조회합니다. 각 댓글의 대댓글 개수 포함.")
    @GetMapping
    public ResponseEntity<Page<CommentListResponseDto>> getRootComments(
            @RequestParam Long diaryId,
            @ParameterObject
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC, size = 10) Pageable pageable, HttpServletRequest request) {
        log.info("일기 {}의 원댓글 조회 요청, page: {}, size: {}", diaryId, pageable.getPageNumber(), pageable.getPageSize());

        RequestMetaInfo requestMetaInfo = requestMetaMapper.extractMetaInfo(request);
        Page<CommentListResponseDto> rootCommentsPage = commentService.getRootCommentsByDiaryId(diaryId, pageable ,requestMetaInfo);

        return ResponseEntity.status(HttpStatus.OK).body(rootCommentsPage);
    }

    @Operation(summary = "대댓글 조회", description = "특정 부모 댓글의 대댓글 목록을 페이징하여 조회합니다.")
    @GetMapping("/{parentCommentId}/replies")
    public ResponseEntity<Page<CommentListResponseDto>> getReplies(
            @PathVariable Long parentCommentId,
            @ParameterObject
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.ASC, size = 10) Pageable pageable, HttpServletRequest request) {
        log.info("부모 댓글 {}에 대한 대댓글 조회 요청, page: {}, size: {}", parentCommentId, pageable.getPageNumber(), pageable.getPageSize());

        RequestMetaInfo requestMetaInfo = requestMetaMapper.extractMetaInfo(request);
        Page<CommentListResponseDto> repliesPage = commentService.getRepliesByParentCommentId(parentCommentId, pageable, requestMetaInfo);

        return ResponseEntity.ok(repliesPage);
    }

    @Operation(summary = "댓글 삭제", description = "댓글을 삭제합니다.")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<CommentDeleteResponseDto> deleteComment(@PathVariable Long commentId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("사용자 {}님이 {} 댓글 삭제 요청", userDetails.getId(), commentId);
        CommentDeleteResponseDto isDeleted = commentService.deleteComment(commentId, userDetails.getId());

        return ResponseEntity.status(HttpStatus.OK).body(isDeleted);
    }

}
