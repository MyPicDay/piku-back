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

    @Setter
    private int replyCount;


    public static CommentListResponseDto fromEntity(Comment comment) {
        String nickname = (comment.getUser() != null) ? comment.getUser().getNickname() : "me";
        String profileImage = (comment.getUser() != null) ? comment.getUser().getAvatar() : null;

        return CommentListResponseDto.builder()
                .id(comment.getId())
                .diaryId(comment.getDiary() != null ? comment.getDiary().getId() : null)
                .userId(comment.getUser() != null ? comment.getUser().getId() : null)
                .nickname(nickname)
                .avatar(profileImage)
                .content(comment.getContent())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .createdAt(comment.getCreatedAt())
                .build();
    }

}
