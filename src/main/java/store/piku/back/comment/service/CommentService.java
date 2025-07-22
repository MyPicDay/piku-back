package store.piku.back.comment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.piku.back.comment.dto.request.CommentRequestDto;
import store.piku.back.comment.dto.request.CommentUpdateDto;
import store.piku.back.comment.dto.response.CommentDeleteResponseDto;
import store.piku.back.comment.dto.response.CommentListResponseDto;
import store.piku.back.comment.dto.response.CommentResponseDto;
import store.piku.back.comment.entity.Comment;
import store.piku.back.comment.exception.CommentErrorCode;
import store.piku.back.comment.exception.CommentException;
import store.piku.back.comment.repository.CommentRepository;
import store.piku.back.diary.entity.Diary;
import store.piku.back.diary.exception.DiaryNotFoundException;
import store.piku.back.diary.service.DiaryService;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.global.util.ImagePathToUrlConverter;
import store.piku.back.user.entity.User;
import store.piku.back.user.service.reader.UserReader;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final UserReader userReader;
    private final CommentRepository commentRepository;
    private final DiaryService diaryService;
    private final ImagePathToUrlConverter imagePathToUrlConverter;


    /**
     * parentId가 Null이면 원댓글, parentId가 있으면 그에 딸린 대댓글을 등록합니다.
     *
     * @param commentRequestDto 댓글 등록 dto
     * @param userId 댓글 작성자 id
     * @return ResponseCommentDto 댓글 등록 dto
    * */
    @Transactional
    public CommentResponseDto createComment(CommentRequestDto commentRequestDto, String userId) throws DiaryNotFoundException {

        User user = userReader.getUserById(userId);
        Diary diary = diaryService.getDiaryById(commentRequestDto.getDiaryId());
        Comment comment = new Comment(commentRequestDto.getContent(), user, diary);

        if (commentRequestDto.getParentId() != null) { //대댓글인 경우
            Comment parentComment = validateCommentExists(commentRequestDto.getParentId());
            validateCommentNotDeleted(parentComment);

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
        userReader.getUserById(userId);
        Comment comment = validateCommentForEditOrDelete(commentId, userId);
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
     * 댓글 식별값 기준으로 댓글을 삭제합니다.
     *
     * @param commentId 수정할 댓글 ID
     * @param userId 댓글 작성자 ID
     * @return ResponseCommentDto 수정된 댓글 정보 DTO
     * @throws CommentException 댓글 커스텀 예외
     * */
    @Transactional
    public CommentDeleteResponseDto deleteComment(Long commentId, String userId) throws CommentException{
        userReader.getUserById(userId);
        Comment comment = validateCommentForEditOrDelete(commentId, userId);
        comment.inactive();
        commentRepository.save(comment);
        log.info("사용자 {}님이 댓글 {} 삭제 완료", userId, commentId);

        return new CommentDeleteResponseDto(true,"성공적으로 댓글을 삭제하였습니다.", commentId);
    }

    /**
     * 특정 일기의 루트 댓글을 페이징하여 조회하고, 각 댓글의 대댓글 개수를 포함합니다.
     *
     * @param diaryId 다이어리 식별값
     * @param pageable 페이지 정보
     * @return 페이징된 CommentListResponseDto 목록
     */
    @Transactional(readOnly = true)
    public Page<CommentListResponseDto> getRootCommentsByDiaryId(Long diaryId, Pageable pageable , RequestMetaInfo requestMetaInfo) {

        diaryService.getDiaryById(diaryId);
        Page<Comment> rootCommentsPage = commentRepository.findVisibleRootCommentsByDiaryId(diaryId, pageable);
        log.info("일기 ID {}에 대한 {} 페이지, {}개 크기의 루트 댓글 {}개 조회 완료.", diaryId, pageable.getPageNumber(), pageable.getPageSize(), rootCommentsPage.getTotalElements());

        return rootCommentsPage.map(rootComment -> {

            String avatarUrl = null;
            if (rootComment.getDeletedAt() == null) {
                avatarUrl = imagePathToUrlConverter.userAvatarImageUrl(rootComment.getUser().getAvatar(), requestMetaInfo);
            }

            int replyCount = commentRepository.countByParentIdAndDeletedAtIsNull(rootComment.getId());

            return CommentListResponseDto.fromEntity(rootComment, avatarUrl, replyCount);
        });
    }

    /**
     * 특정 부모 댓글의 대댓글 목록을 페이징하여 조회합니다.
     *
     * @param parentCommentId 부모 댓글 식별값
     * @param pageable 페이지 정보
     * @return 페이징된 CommentListResponseDto 목록
     */
    @Transactional(readOnly = true)
    public Page<CommentListResponseDto> getRepliesByParentCommentId(Long parentCommentId, Pageable pageable , RequestMetaInfo requestMetaInfo) {

        Comment parentComment = validateCommentExists(parentCommentId);
        diaryService.getDiaryById(parentComment.getDiary().getId());
        Page<Comment> repliesPage = commentRepository.findByParentIdAndDeletedAtIsNull(parentCommentId, pageable);
        log.info("부모 댓글 ID {}에 대한 {} 페이지, {}개 크기의 대댓글 {}개 조회 완료.", parentCommentId, pageable.getPageNumber(), pageable.getPageSize(), repliesPage.getTotalElements());

        return repliesPage.map(replyComment -> {
            String avatarUrl = imagePathToUrlConverter.userAvatarImageUrl(replyComment.getUser().getAvatar(), requestMetaInfo);
            return CommentListResponseDto.fromEntity(replyComment, avatarUrl, 0);
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

    /**
     * 주어진 ID로 댓글을 찾음
     *
     * @param commentId 찾을 댓글 ID
     * @return 찾은 Comment 엔티티
     * @throws CommentException 댓글을 찾을 수 없을 경우
     */
    private Comment validateCommentExists(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> {
                    log.error("댓글 ID {}을(를) 찾을 수 없습니다.", commentId);
                    return new CommentException(CommentErrorCode.COMMENT_NOT_FOUND);});
    }

    /**
     * 주어진 ID로 댓글이 삭제되었는지 확인
     *
     * @param comment 삭제되었는지 확인할 댓글
     * @throws CommentException 댓글이 삭제된 경우
     */
    private void validateCommentNotDeleted(Comment comment) {
        if (comment.isDeleted()) {
            log.warn("이미 삭제된 댓글입니다. commentId={}", comment.getId());
            throw new CommentException(CommentErrorCode.DELETED_COMMENT);
        }
    }

    /**
     * 댓글 수정, 삭제 전 시행할 유효성 검사
     *
     * @param userId  로그에 남길 사용자 ID
     * @param commentId 유효성 검사를 실시할 댓글 ID
     * @return 유효성 검사를 실시한 Comment 엔티티
     * @throws CommentException 유효성 검사에 실패하는 경우
     */
    private Comment validateCommentForEditOrDelete(Long commentId, String userId) throws CommentException {
        Comment comment = validateCommentExists(commentId);
        validateCommentNotDeleted(comment);

        if (!comment.getUser().getId().equals(userId)) {
            log.error("본인의 댓글만 수정/삭제할 수 있습니다. commentId={}, userId={}", commentId, userId);
            throw new CommentException(CommentErrorCode.UNAUTHORIZED_ACCESS);
        }

        if (comment.getDiary() != null) {
            diaryService.getDiaryById(comment.getDiary().getId());
        } else {
            // 댓글이 어떤 다이어리에도 속해있지 않은 경우
            log.error("댓글 {}이 연결된 다이어리를 찾을 수 없습니다.", commentId);
            throw new CommentException(CommentErrorCode.INVALID_REQUEST);
        }

        return comment;
    }

}
