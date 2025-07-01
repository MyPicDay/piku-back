package store.piku.back.comment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import store.piku.back.comment.dto.request.CommentRequestDto;
import store.piku.back.comment.dto.request.CommentUpdateDto;
import store.piku.back.comment.dto.response.CommentResponseDto;
import store.piku.back.comment.service.CommentService;
import store.piku.back.global.config.CustomUserDetails;

@Tag(name = "Comment", description = "댓글 관련 API")
@RestController
@Slf4j
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "댓글 작성", description = "댓글 작성")
    @SecurityRequirement(name = "JWT")
    @PostMapping
    public ResponseEntity<CommentResponseDto> createComment(@RequestBody CommentRequestDto commentRequestDto, @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("사용자 {}님이 {} 일기, {} 댓글에 댓글 등록 요청, 댓글 내용: {}", userDetails.getId(), commentRequestDto.getDiaryId(), commentRequestDto.getParentId(), commentRequestDto.getContent());
        CommentResponseDto isSaved = commentService.createComment(commentRequestDto, userDetails.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(isSaved);
    }

    @Operation(summary = "댓글 수정", description = "댓글 수정")
    @PatchMapping("/{commentId}")
    public ResponseEntity<CommentResponseDto> updateComment(@PathVariable Long commentId, @RequestBody CommentUpdateDto updateDto, @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("사용자 {}님이 {} 댓글 수정 요청, 수정할 댓글 내용: {}", userDetails.getId(), commentId, updateDto.getContent());
        CommentResponseDto isSaved = commentService.updateComment(commentId, updateDto, userDetails.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(isSaved);
    }

}
