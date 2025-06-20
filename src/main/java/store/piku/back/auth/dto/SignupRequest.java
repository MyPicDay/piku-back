package store.piku.back.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "회원가입 요청 DTO")
public class SignupRequest {

    @Schema(description = "사용자 이메일", example = "test@gmail.com")
    private String email;
    @Schema(description = "사용자 패스워드", example = "1")
    private String password;
    @Schema(description = "사용자 이름", example = "테스트유저")
    private String nickname;
    @Schema(description = "고정 캐릭터 ID", example = "1")
    private Long fixedCharacterId;
}
