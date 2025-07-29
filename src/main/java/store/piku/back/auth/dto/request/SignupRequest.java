package store.piku.back.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "회원가입 요청 DTO")
public class SignupRequest {

    @Email
    @Schema(description = "사용자 이메일", example = "test@gmail.com")
    private String email;

    @NotBlank
    @Pattern(regexp = "^(?=.*[!@#$%^&*])[A-Za-z0-9!@#$%^&*]+$", message = "비밀번호는 특수문자를 적어도 한 번 포함해야 합니다.")
    @Schema(description = "사용자 패스워드", example = "abc@123")
    private String password;

    @NotBlank
    @Schema(description = "사용자 이름", example = "테스트유저")
    private String nickname;

    @NotNull
    @Schema(description = "고정 캐릭터 ID", example = "1")
    private Long fixedCharacterId;
}
