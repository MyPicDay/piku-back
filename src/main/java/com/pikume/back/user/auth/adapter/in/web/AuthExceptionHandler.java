package com.pikume.back.user.auth.adapter.in.web;

import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.user.auth.adapter.in.web.problem.AuthProblemType;
import com.pikume.back.user.auth.application.exception.AuthErrorCode;
import com.pikume.back.user.auth.application.exception.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.pikume.back.user.auth")
@RequiredArgsConstructor
public class AuthExceptionHandler {
	private final ProblemDetailFactory problemDetailFactory;

	@ExceptionHandler(AuthException.class)
	public ResponseEntity<ProblemDetail> handleAuthException(AuthException exception, HttpServletRequest request) {
		AuthProblemType type = mapProblemType(exception.getErrorCode());
		return ResponseEntity.status(type.status()).body(
				problemDetailFactory.create(type, exception.getMessage(), request.getRequestURI()));
	}

	private AuthProblemType mapProblemType(AuthErrorCode code) {
		return switch (code) {
			case USER_NOT_FOUND, VERIFICATION_NOT_FOUND -> AuthProblemType.USER_NOT_FOUND;
			case FIXED_CHARACTER_NOT_FOUND -> AuthProblemType.FIXED_CHARACTER_NOT_FOUND;
			case EMAIL_ALREADY_EXISTS -> AuthProblemType.EMAIL_ALREADY_EXISTS;
			case NICKNAME_ALREADY_EXISTS -> AuthProblemType.NICKNAME_CONFLICT;
			case EMAIL_VERIFICATION_NOT_FOUND -> AuthProblemType.EMAIL_VERIFICATION_REQUIRED;
			case EMAIL_SEND_FAILURE -> AuthProblemType.EMAIL_SEND_FAILURE;
			case CODE_EXPIRED, CODE_MISMATCH, EMAIL_VERIFICATION_EXPIRED,
					EMAIL_VERIFICATION_ALREADY_USED, INVALID_EMAIL -> AuthProblemType.CODE_INVALID;
		};
	}
}
