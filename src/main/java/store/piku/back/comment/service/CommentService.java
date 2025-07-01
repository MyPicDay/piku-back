package store.piku.back.comment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.piku.back.comment.dto.request.CommentRequestDto;
import store.piku.back.comment.dto.request.CommentUpdateDto;
import store.piku.back.comment.dto.response.CommentResponseDto;
import store.piku.back.comment.entity.Comment;
import store.piku.back.comment.exception.CommentErrorCode;
import store.piku.back.comment.exception.CommentException;
import store.piku.back.comment.repository.CommentRepository;
import store.piku.back.diary.entity.Diary;
import store.piku.back.diary.exception.DiaryNotFoundException;
import store.piku.back.diary.repository.DiaryRepository;
import store.piku.back.user.entity.User;
import store.piku.back.user.service.UserService;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final UserService userService;
    private final DiaryRepository diaryRepository;
    private final CommentRepository commentRepository;


    /**
     * 댓글 등록
     *
     * @param commentRequestDto 댓글 등록 dto
     * @param userId 댓글 작성자 id
     * @return ResponseCommentDto 댓글 등록 dto
    * */
    @Transactional
    public CommentResponseDto createComment(CommentRequestDto commentRequestDto, String userId) throws DiaryNotFoundException {

        User user = userService.getUserById(userId);
        Diary diary = diaryRepository.findById(commentRequestDto.getDiaryId())
                .orElseThrow(DiaryNotFoundException::new);

        Comment comment = new Comment(commentRequestDto.getContent(), user, diary);

        if (commentRequestDto.getParentId() != null) {
            Comment parentComment = commentRepository.findById(commentRequestDto.getParentId())
                    .orElseThrow(() -> new CommentException(CommentErrorCode.COMMENT_NOT_FOUND));

            if (parentComment.getParent() != null) {
                throw new CommentException(CommentErrorCode.INVALID_PARENT_COMMENT);
            }
            comment.setParent(parentComment);
        }

        Comment savedComment;
        try {
            savedComment = commentRepository.save(comment);
        } catch (DataAccessException e) {
            log.error("DB 댓글 저장 실패. user: {}, diaryId: {}", userId, diary.getId(), e);
            throw new CommentException(CommentErrorCode.DATABASE_ERROR, e);
        }

        log.info("사용자 {}님이 {} 일기에 댓글 등록 완료, 댓글 내용: {}", savedComment.getUser().getNickname(), savedComment.getDiary().getId(), savedComment.getContent());

        return new CommentResponseDto(
                savedComment.getId(),
                savedComment.getContent(),
                savedComment.getCreatedAt()
        );
    }


    /**
     * 댓글 수정
     *
     * @param commentId 수정할 댓글 ID
     * @param commentRequestDto 수정할 댓글 내용이 담긴 DTO
     * @param userId 댓글 작성자 ID
     * @return ResponseCommentDto 수정된 댓글 정보 DTO
     * @throws CommentException 댓글 커스텀 예외
     * */
    @Transactional
    public CommentResponseDto updateComment(Long commentId, CommentUpdateDto commentRequestDto, String userId) throws CommentException{

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentException(CommentErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getUser().getId().equals(userId)) {
//            log.error();
            throw new CommentException(CommentErrorCode.UNAUTHORIZED_ACCESS);
        }

        if (comment.getDiary() != null) {
            diaryRepository.findById(comment.getDiary().getId())
                    .orElseThrow(DiaryNotFoundException::new);
        } else {
            // 댓글이 어떤 다이어리에도 속해있지 않은 경우
            log.error("댓글 {}이(가) 연결된 다이어리를 찾을 수 없습니다.", commentId);
            throw new CommentException(CommentErrorCode.INVALID_REQUEST);
        }

        comment.updateContent(commentRequestDto.getContent());
        Comment updatedComment;

        try {
            updatedComment = commentRepository.save(comment);
        } catch (DataAccessException e) {
            log.error("DB 댓글 수정 실패. commentId: {}, userId: {}", commentId, userId, e);
            throw new CommentException(CommentErrorCode.DATABASE_ERROR, e);
        }

        log.info("사용자 {}님이 댓글 {} 수정 완료, 수정 내용: {}", userId, updatedComment.getId(), updatedComment.getContent());

        return new CommentResponseDto(
                updatedComment.getId(),
                updatedComment.getContent(),
                updatedComment.getCreatedAt()
        );
    }
}
