package store.piku.back.comment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.piku.back.comment.dto.request.CommentRequestDto;
import store.piku.back.comment.dto.request.CommentUpdateDto;
import store.piku.back.comment.dto.response.CommentListResponseDto;
import store.piku.back.comment.dto.response.CommentResponseDto;
import store.piku.back.comment.entity.Comment;
import store.piku.back.comment.exception.CommentErrorCode;
import store.piku.back.comment.exception.CommentException;
import store.piku.back.comment.repository.CommentRepository;
import store.piku.back.diary.entity.Diary;
import store.piku.back.diary.exception.DiaryNotFoundException;
import store.piku.back.diary.service.DiaryService;
import store.piku.back.user.entity.User;
import store.piku.back.user.service.UserService;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final UserService userService;
    private final CommentRepository commentRepository;
    private final DiaryService diaryService;


    /**
     * parentId가 Null이면 원댓글, parentId가 있으면 그에 딸린 대댓글을 등록합니다.
     *
     * @param commentRequestDto 댓글 등록 dto
     * @param userId 댓글 작성자 id
     * @return ResponseCommentDto 댓글 등록 dto
    * */
    @Transactional
    public CommentResponseDto createComment(CommentRequestDto commentRequestDto, String userId) throws DiaryNotFoundException {

        User user = userService.getUserById(userId);
        Diary diary = diaryService.getDiaryById(commentRequestDto.getDiaryId());

        Comment comment = new Comment(commentRequestDto.getContent(), user, diary);

        if (commentRequestDto.getParentId() != null) {
            Comment parentComment = findCommentById(commentRequestDto.getParentId());

            if (parentComment.getParent() != null) {
                throw new CommentException(CommentErrorCode.INVALID_PARENT_COMMENT);
            }

            if (!parentComment.getDiary().getId().equals(diary.getId())) {
                throw new CommentException(CommentErrorCode.PARENT_COMMENT_NOT_IN_SAME_DIARY);
            }

            comment.connectParent(parentComment);
        }

        Comment savedComment = saveCommentToDb(comment, userId, diary.getId());
        log.info("사용자 {}님이 {} 일기에 댓글 등록 완료, 댓글 내용: {}", savedComment.getUser().getNickname(), savedComment.getDiary().getId(), savedComment.getContent());

        return new CommentResponseDto(
                savedComment.getId(),
                savedComment.getContent(),
                savedComment.getCreatedAt()
        );
    }


    /**
     * 댓글 식별값 기준으로 댓글 내용을 수정합니다.
     *
     * @param commentId 수정할 댓글 ID
     * @param commentRequestDto 수정할 댓글 내용이 담긴 DTO
     * @param userId 댓글 작성자 ID
     * @return ResponseCommentDto 수정된 댓글 정보 DTO
     * @throws CommentException 댓글 커스텀 예외
     * */
    @Transactional
    public CommentResponseDto updateComment(Long commentId, CommentUpdateDto commentRequestDto, String userId) throws CommentException{
        Comment comment = findCommentById(commentId);

        if (!comment.getUser().getId().equals(userId)) {
            log.error("본인의 댓글만 수정할 수 있습니다.");
            throw new CommentException(CommentErrorCode.UNAUTHORIZED_ACCESS);
        }

        if (comment.getDiary() != null) {
            diaryService.getDiaryById(comment.getDiary().getId());
        } else {
            // 댓글이 어떤 다이어리에도 속해있지 않은 경우
            log.error("댓글 {}이 연결된 다이어리를 찾을 수 없습니다.", commentId);
            throw new CommentException(CommentErrorCode.INVALID_REQUEST);
        }

        comment.updateContent(commentRequestDto.getContent());
        Comment updatedComment = saveCommentToDb(comment, userId, comment.getDiary().getId());

        log.info("사용자 {}님이 댓글 {} 수정 완료, 수정 내용: {}", userId, updatedComment.getId(), updatedComment.getContent());

        return new CommentResponseDto(
                updatedComment.getId(),
                updatedComment.getContent(),
                updatedComment.getCreatedAt()
        );
    }

    /**
     * 특정 일기의 루트 댓글을 페이징하여 조회하고, 각 댓글의 대댓글 개수를 포함합니다.
     *
     * @param diaryId 일기 ID
     * @param page    페이지 번호 (0부터 시작)
     * @param size    한 페이지당 댓글 수
     * @return 페이징된 CommentListResponseDto 목록
     */
    @Transactional(readOnly = true)
    public Page<CommentListResponseDto> getRootCommentsByDiaryId(Long diaryId, int page, int size) {

        log.info("원댓글 조회 시작: diaryId={}, page={}, size={}", diaryId, page, size);
        diaryService.getDiaryById(diaryId);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Comment> rootCommentsPage = commentRepository.findByDiaryIdAndParentIsNull(diaryId, pageable);
        log.info("일기 ID {}에 대한 {} 페이지, {}개 크기의 루트 댓글 {}개 조회 완료.", diaryId, page, size, rootCommentsPage.getTotalElements());

        return rootCommentsPage.map(rootComment -> {
            CommentListResponseDto dto = CommentListResponseDto.fromEntity(rootComment);
            dto.setReplyCount(commentRepository.countByParentId(rootComment.getId()));
            return dto;
        });
    }

    /**
     * 특정 부모 댓글의 대댓글 목록을 페이징하여 조회합니다.
     *
     * @param parentCommentId 부모 댓글 ID
     * @param page            페이지 번호 (0부터 시작)
     * @param size            한 페이지당 대댓글 수
     * @return 페이징된 CommentListResponseDto 목록
     */
    @Transactional(readOnly = true)
    public Page<CommentListResponseDto> getRepliesByParentCommentId(Long parentCommentId, int page, int size) {

        log.info("대댓글 조회 시작: parentCommentId={}, page={}, size={}", parentCommentId, page, size);
        Comment comment = findCommentById(parentCommentId);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        Page<Comment> repliesPage = commentRepository.findByParentId(parentCommentId, pageable);
        log.info("부모 댓글 ID {}에 대한 {} 페이지, {}개 크기의 대댓글 {}개 조회 완료.", parentCommentId, page, size, repliesPage.getTotalElements());

        return repliesPage.map(replyComment -> {
            CommentListResponseDto dto = CommentListResponseDto.fromEntity(replyComment);
            dto.setReplyCount(0);
            return dto;
        });
    }

    /**
     * diaryId 를 기준으로 댓글과 대댓글을 포함한 전체 개수를 반환합니다.
     *
     * @param diaryId 다이어리 ID
     * @return 댓글과 대댓글의 전체 개수
     */
    @Transactional(readOnly = true)
    public long countAllCommentsByDiaryId(Long diaryId) {
        diaryService.getDiaryById(diaryId);

        long count = commentRepository.countAllByDiaryId(diaryId);
        log.info("일기 ID {}에 달린 전체 댓글 수: {}", diaryId, count);
        return count;
    }








    /**
     * 주어진 ID로 댓글을 찾음
     *
     * @param commentId 찾을 댓글 ID
     * @return 찾은 Comment 엔티티
     * @throws CommentException 댓글을 찾을 수 없을 경우
     */
    private Comment findCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> {
                    log.error("댓글 ID {}을(를) 찾을 수 없습니다.", commentId);
                    return new CommentException(CommentErrorCode.COMMENT_NOT_FOUND);});
    }

    /**
     * 댓글을 DB에 저장
     *
     * @param comment 저장할 Comment 엔티티
     * @param userId  로그에 남길 사용자 ID
     * @param diaryId 로그에 남길 다이어리 ID
     * @return 저장된 Comment 엔티티
     * @throws CommentException db 작업에 실패하는 경우
     */
    private Comment saveCommentToDb(Comment comment, String userId, Long diaryId) {
        try {
            return commentRepository.save(comment);
        } catch (DataAccessException e) {
            log.error("DB 댓글 저장/수정 실패. user: {}, diaryId: {}, commentId: {}", userId, diaryId, comment.getId(), e);
            throw new CommentException(CommentErrorCode.DATABASE_ERROR, e);
        }
    }

}
