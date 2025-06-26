package store.piku.back.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "댓글 작성 DTO")
public class CommentDto {

    @Schema(description = "일기 id")
    private Long diaryId ;

    @Schema(description = "댓글 내용")
    private String content ;
}
