package com.pikume.back.social.adapter.in.web;

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
import com.pikume.back.global.config.CustomUserDetails;
import com.pikume.back.global.dto.RequestMetaInfo;
import com.pikume.back.global.pagination.PageQuery;
import com.pikume.back.global.pagination.PageResult;
import com.pikume.back.global.pagination.SpringPageMapper;
import com.pikume.back.global.util.RequestMetaMapper;
import com.pikume.back.social.adapter.in.web.dto.CommentDeleteResponseDto;
import com.pikume.back.social.application.dto.CommentDeleteResult;
import com.pikume.back.social.application.dto.CommentListItemResult;
import com.pikume.back.social.application.dto.CommentResult;
import com.pikume.back.social.adapter.in.web.dto.CommentListResponseDto;
import com.pikume.back.social.adapter.in.web.dto.CommentRequestDto;
import com.pikume.back.social.adapter.in.web.dto.CommentResponseDto;
import com.pikume.back.social.adapter.in.web.dto.CommentUpdateDto;
import com.pikume.back.social.application.port.in.CommentUseCase;

@Tag(name = "Comment", description = "댓글 관련 API")
@RestController
@Slf4j
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

	private final CommentUseCase commentUseCase;
	private final RequestMetaMapper requestMetaMapper;

	@Operation(summary = "댓글 작성", description = "댓글을 작성합니다.")
	@SecurityRequirement(name = "JWT")
	@PostMapping
	public ResponseEntity<CommentResponseDto> createComment(@RequestBody CommentRequestDto commentRequestDto,
			@AuthenticationPrincipal CustomUserDetails userDetails, HttpServletRequest request) {
		RequestMetaInfo requestMetaInfo = requestMetaMapper.extractMetaInfo(request);

		log.info("사용자 {}님이 {} 일기, {} 댓글에 댓글 등록 요청, 댓글 내용: {}", userDetails.getId(), commentRequestDto.getDiaryId(),
				commentRequestDto.getParentId(), commentRequestDto.getContent());
		CommentResult isSaved = commentUseCase.createComment(
				commentRequestDto.getDiaryId(),
				commentRequestDto.getContent(),
				commentRequestDto.getParentId(),
				userDetails.getId(),
				requestMetaInfo);

		return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(isSaved));
	}

	@Operation(summary = "댓글 수정", description = "댓글 내용을 수정합니다.")
	@PatchMapping("/{commentId}")
	public ResponseEntity<CommentResponseDto> updateComment(@PathVariable Long commentId,
			@RequestBody CommentUpdateDto updateDto, @AuthenticationPrincipal CustomUserDetails userDetails) {
		log.info("사용자 {}님이 {} 댓글 수정 요청, 수정할 댓글 내용: {}", userDetails.getId(), commentId, updateDto.getContent());
		CommentResult isSaved = commentUseCase.updateComment(commentId, updateDto.getContent(), userDetails.getId());

		return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(isSaved));
	}

	@Operation(summary = "원댓글 조회", description = "특정 일기의 루트 댓글을 페이징하여 조회합니다. 각 댓글의 대댓글 개수 포함.")
	@GetMapping
	public ResponseEntity<Page<CommentListResponseDto>> getRootComments(
			@RequestParam Long diaryId,
			@ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC, size = 10) Pageable pageable,
			HttpServletRequest request,
			@AuthenticationPrincipal CustomUserDetails userDetails) {
		log.info("일기 {}의 원댓글 조회 요청, page: {}, size: {}", diaryId, pageable.getPageNumber(), pageable.getPageSize());

		RequestMetaInfo requestMetaInfo = requestMetaMapper.extractMetaInfo(request);
		String viewerId = userDetails != null ? userDetails.getId() : null;
		PageQuery pageQuery = SpringPageMapper.toPageQuery(pageable);
		PageResult<CommentListResponseDto> rootCommentResults = commentUseCase.getRootCommentsByDiaryId(diaryId, pageQuery,
				requestMetaInfo, viewerId)
				.map(this::toResponse);
		Page<CommentListResponseDto> rootCommentsPage = SpringPageMapper.toSpringPage(rootCommentResults, pageable);

		return ResponseEntity.status(HttpStatus.OK).body(rootCommentsPage);
	}

	@Operation(summary = "대댓글 조회", description = "특정 부모 댓글의 대댓글 목록을 페이징하여 조회합니다.")
	@GetMapping("/{parentCommentId}/replies")
	public ResponseEntity<Page<CommentListResponseDto>> getReplies(
			@PathVariable Long parentCommentId,
			@ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.ASC, size = 10) Pageable pageable,
			HttpServletRequest request,
			@AuthenticationPrincipal CustomUserDetails userDetails) {
		log.info("부모 댓글 {}에 대한 대댓글 조회 요청, page: {}, size: {}", parentCommentId, pageable.getPageNumber(),
				pageable.getPageSize());

		RequestMetaInfo requestMetaInfo = requestMetaMapper.extractMetaInfo(request);
		String viewerId = userDetails != null ? userDetails.getId() : null;
		PageQuery pageQuery = SpringPageMapper.toPageQuery(pageable);
		PageResult<CommentListResponseDto> replyResults = commentUseCase.getRepliesByParentCommentId(parentCommentId, pageQuery,
				requestMetaInfo, viewerId)
				.map(this::toResponse);
		Page<CommentListResponseDto> repliesPage = SpringPageMapper.toSpringPage(replyResults, pageable);

		return ResponseEntity.ok(repliesPage);
	}

	@Operation(summary = "댓글 삭제", description = "댓글을 삭제합니다.")
	@DeleteMapping("/{commentId}")
	public ResponseEntity<CommentDeleteResponseDto> deleteComment(@PathVariable Long commentId,
			@AuthenticationPrincipal CustomUserDetails userDetails) {
		log.info("사용자 {}님이 {} 댓글 삭제 요청", userDetails.getId(), commentId);
		CommentDeleteResult isDeleted = commentUseCase.deleteComment(commentId, userDetails.getId());

		return ResponseEntity.status(HttpStatus.OK).body(toResponse(isDeleted));
	}

	private CommentResponseDto toResponse(CommentResult result) {
		return new CommentResponseDto(result.id(), result.content(), result.createdAt());
	}

	private CommentDeleteResponseDto toResponse(CommentDeleteResult result) {
		return new CommentDeleteResponseDto(result.success(), result.message(), result.commentId());
	}

	private CommentListResponseDto toResponse(CommentListItemResult result) {
		return new CommentListResponseDto(
				result.id(),
				result.diaryId(),
				result.userId(),
				result.nickname(),
				result.avatar(),
				result.content(),
				result.parentId(),
				result.createdAt(),
				result.updatedAt(),
				result.replyCount());
	}
}
