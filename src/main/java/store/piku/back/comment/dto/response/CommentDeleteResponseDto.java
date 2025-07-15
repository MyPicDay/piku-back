package store.piku.back.comment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CommentDeleteResponseDto {
    private boolean success;
    private String message;
    private Long commentId;

}
