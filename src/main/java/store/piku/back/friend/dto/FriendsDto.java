package store.piku.back.friend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class FriendsDto {
    @Schema(description = "친구의 사용자 ID", example = "friend123")
    private String userId;

    @Schema(description = "친구의 닉네임", example = "개발자친구")
    private String nickname;

    @Schema(description = "친구의 아바타 URL 또는 이미지 경로", example = "https://example.com/avatar.jpg")
    private String avatar;
}
