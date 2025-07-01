package store.piku.back.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "댓글 작성 DTO")
public class CommentRequestDto {

    @Schema(description = "일기 식별값")
    private Long diaryId ;

    @Schema(description = "댓글 내용")
    private String content ;

    @Schema(description = "원 댓글 식별값")
    private Long parentId;
}
