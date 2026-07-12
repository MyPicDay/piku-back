package com.pikume.back.user.auth.adapter.in.web.dto.request;

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
public class SignupRequest {
	@Email
	private String email;
	@NotBlank
	@Pattern(regexp = "^(?=.*[!@#$%^&*])[A-Za-z0-9!@#$%^&*]+$")
	private String password;
	@NotBlank
	private String nickname;
	@NotNull
	private Long fixedCharacterId;
}
