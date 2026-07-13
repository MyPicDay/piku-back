package com.pikume.back.user.auth.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PwdResetRequest {
	@NotBlank(message = "이메일은 필수 값입니다.")
	private String email;
	@NotBlank(message = "비밀번호는 필수 값입니다.")
	private String password;
}
