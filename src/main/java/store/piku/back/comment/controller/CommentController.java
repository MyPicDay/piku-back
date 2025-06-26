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
import store.piku.back.comment.dto.CommentDto;
import store.piku.back.comment.dto.response.ResponseCommentDto;
import store.piku.back.comment.service.CommentService;
import store.piku.back.global.config.CustomUserDetails;

@Tag(name = "Comment", description = "댓글 관련 API")
@RestController
@Slf4j
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "댓글 작성", description = "원댓글 작성")
    @SecurityRequirement(name = "JWT")
    @PostMapping
    public ResponseEntity<ResponseCommentDto> createComment(@RequestBody CommentDto commentDto, @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("사용자 {}님이 {} 일기에 댓글 등록 요청, 댓글 내용: {}", userDetails.getId(), commentDto.getDiaryId(), commentDto.getContent());
        ResponseCommentDto isSaved = commentService.createComment(commentDto, userDetails.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(isSaved);
    }

}
