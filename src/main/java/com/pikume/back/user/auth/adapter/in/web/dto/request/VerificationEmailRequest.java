package com.pikume.back.user.auth.adapter.in.web.dto.request;

import com.pikume.back.user.auth.adapter.in.web.validation.EmailFormat;
import jakarta.validation.constraints.NotBlank;

public record VerificationEmailRequest(
		@NotBlank(message = "이메일은 필수 값입니다.")
		@EmailFormat
		String email) {
}
