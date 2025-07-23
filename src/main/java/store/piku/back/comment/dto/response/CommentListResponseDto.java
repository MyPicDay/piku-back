package store.piku.back.comment.dto.response;

import lombok.*;
import store.piku.back.comment.entity.Comment;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentListResponseDto {
    private Long id;
    private Long diaryId;
    private String userId;
    private String nickname;
    private String avatar;
    private String content;
    private Long parentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private int replyCount;


    public static CommentListResponseDto fromEntity(Comment comment, String avatarUrl, int replyCount) {
        String nickname = (comment.getUser() != null) ? comment.getUser().getNickname() : "me";

        return CommentListResponseDto.builder()
                .id(comment.getId())
                .diaryId(comment.getDiary() != null ? comment.getDiary().getId() : null)
                .userId(!comment.isDeleted() && comment.getUser() != null ? comment.getUser().getId() : null)
                .nickname(comment.isDeleted() ? null : nickname)
                .avatar(comment.isDeleted() ? null : avatarUrl)
                .content(comment.isDeleted() ? "삭제된 댓글입니다." : comment.getContent())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .replyCount(replyCount)
                .build();
    }

}
