package store.piku.back.friend.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class FriendRequestDto {

    @Schema(description = "친구 요청을 보낼 대상 사용자의 ID", example = "user456")
    private String toUserId;
}
