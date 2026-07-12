package com.pikume.back.user.auth.application.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode {
	USER_NOT_FOUND("가입되지 않은 이메일입니다."),
	VERIFICATION_NOT_FOUND("인증 요청을 찾을 수 없습니다."),
	FIXED_CHARACTER_NOT_FOUND("존재하지 않는 캐릭터입니다."),
	EMAIL_ALREADY_EXISTS("이미 가입된 이메일입니다."),
	NICKNAME_ALREADY_EXISTS("이미 사용 중인 닉네임입니다."),
	EMAIL_VERIFICATION_NOT_FOUND("이메일 인증을 먼저 완료해주세요."),
	CODE_EXPIRED("인증 코드가 만료되었습니다."),
	CODE_MISMATCH("인증 코드가 일치하지 않습니다."),
	EMAIL_VERIFICATION_EXPIRED("이메일 인증이 만료되었습니다."),
	EMAIL_VERIFICATION_ALREADY_USED("이메일 인증이 이미 사용되었습니다."),
	EMAIL_SEND_FAILURE("이메일 발송에 실패했습니다."),
	INVALID_EMAIL("지원하지 않는 이메일 형식입니다.");

	private final String message;
}
