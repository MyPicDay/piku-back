package store.piku.back.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "로그인 DTO")
public class LoginRequest {
    @Schema(description = "사용자 이메일", example = "test@gmail.com")
    private String email;
    @Schema(description = "사용자 비밀번호", example = "1")
    private String password;
}