package store.piku.back.friend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class FriendRequestResponseDto {

    @Schema(description = "친구 요청이 수락 여부", example = "true(false)")
    private boolean isAccepted;
    @Schema(description = "결과 메시지", example = "친구 요청을 보냈습니다. / 친구 요청을 수락했습니다. ")
    private String message;
}
