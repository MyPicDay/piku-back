package com.pikume.back.user.auth.adapter.in.web.dto.request;

import com.pikume.back.user.auth.domain.vo.VerificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "본인 인증 요청 DTO")
public class EmailValidRequest {
	@NotBlank
	@Email
	private String email;
	private String code;
	private VerificationType type;
}
