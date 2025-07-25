package store.piku.back.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import store.piku.back.auth.enums.VerificationType;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "본인 인증 요청 DTO")
public class EmailValidRequest {

    @NotBlank(message = "이메일은 필수 입력 항목입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Schema(description = "사용자 이메일", example = "test@gmail.com")
    private String email;

    @Schema(description = "본인 인증 코드", example = "123456")
    private String code;

    @Schema(description = "이메일 인증 목적", example = "SIGN_UP")
    private VerificationType type;
}
