package com.pikume.back.user.auth.adapter.in.web.dto.request;

import com.pikume.back.user.auth.adapter.in.web.validation.EmailFormat;
import com.pikume.back.user.auth.adapter.in.web.validation.PasswordFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignupRequest {
	@NotBlank(message = "이메일은 필수 값입니다.")
	@EmailFormat
	private String email;
	@NotBlank(message = "비밀번호는 필수 값입니다.")
	@PasswordFormat
	private String password;
	@NotBlank(message = "닉네임은 필수 값입니다.")
	@Size(max = 20, message = "닉네임은 최대 20자까지 입력할 수 있습니다.")
	private String nickname;
	@NotNull(message = "캐릭터 선택은 필수입니다.")
	private Long fixedCharacterId;
}
