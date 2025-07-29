package store.piku.back.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "사용자 프로필 변경 DTO")
public class UpdateProfilePayload {

    @Schema(description = "변경할 닉네임")
    private String newNickname;
    @Schema(description = "변경할 캐릭터 ID")
    private Long characterId;
}
