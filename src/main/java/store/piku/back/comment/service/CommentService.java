package store.piku.back.comment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.piku.back.comment.dto.CommentDto;
import store.piku.back.comment.dto.response.ResponseCommentDto;
import store.piku.back.comment.entity.Comment;
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
     * @param commentDto 댓글 등록 dto
     * @param userId 댓글 작성자 id
     * @return ResponseCommentDto 댓글 등록 dto
    * */
    @Transactional
    public ResponseCommentDto createComment(CommentDto commentDto, String userId) {

        User user = userService.getUserById(userId);
        Diary diary = diaryRepository.findById(commentDto.getDiaryId())
                .orElseThrow(DiaryNotFoundException::new);

        Comment comment = new Comment(commentDto.getContent(), user, diary);

        Comment savedComment = commentRepository.save(comment);
        log.info("사용자 {}님이 {} 일기에 댓글 등록 완료, 댓글 내용: {}", savedComment.getUser().getNickname(), savedComment.getDiary().getId(), savedComment.getContent());

        return new ResponseCommentDto(
                savedComment.getId(),
                savedComment.getContent(),
                savedComment.getCreatedAt()
        );
    }
}
