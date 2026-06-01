package com.pikume.back.user.auth.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.user.auth.adapter.in.web.problem.AuthProblemType;

@RestControllerAdvice(basePackages = "com.pikume.back.user.auth")
@RequiredArgsConstructor
public class AuthExceptionHandler {

	private final ProblemDetailFactory problemDetailFactory;

	@ExceptionHandler(AuthException.class)
	public ResponseEntity<ProblemDetail> handleAuthException(AuthException ex, HttpServletRequest request) {
		AuthProblemType problemType = mapProblemType(ex.getErrorCode());
		ProblemDetail problemDetail = problemDetailFactory.create(problemType, ex.getMessage(), request.getRequestURI());
		return ResponseEntity.status(problemType.status()).body(problemDetail);
	}

	private AuthProblemType mapProblemType(AuthErrorCode errorCode) {
		return switch (errorCode) {
			case USER_NOT_FOUND, VERIFICATION_NOT_FOUND -> AuthProblemType.USER_NOT_FOUND;
			case FIXED_CHARACTER_NOT_FOUND -> AuthProblemType.FIXED_CHARACTER_NOT_FOUND;
			case EMAIL_ALREADY_EXISTS -> AuthProblemType.EMAIL_ALREADY_EXISTS;
			case EMAIL_VERIFICATION_NOT_FOUND -> AuthProblemType.EMAIL_VERIFICATION_REQUIRED;
			case EMAIL_SEND_FAILURE -> AuthProblemType.EMAIL_SEND_FAILURE;
			case CODE_EXPIRED,
					CODE_MISMATCH,
					EMAIL_VERIFICATION_EXPIRED,
					EMAIL_VERIFICATION_ALREADY_USED,
					INVALID_EMAIL -> AuthProblemType.CODE_INVALID;
		};
	}
}
