package com.pikume.back.user.auth.adapter.in.web.dto.request;

import com.pikume.back.user.auth.domain.vo.VerificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "본인 인증 요청 DTO")
public class EmailValidRequest {
	@NotBlank(message = "이메일은 필수 값입니다.")
	private String email;
	@NotBlank(message = "인증 코드는 필수 값입니다.")
	private String code;
	@NotNull(message = "인증 목적은 필수 값입니다.")
	private VerificationType type;
}
