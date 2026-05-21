package com.pikume.back.user.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.user.adapter.in.web.problem.UserProblemType;
import com.pikume.back.user.application.exception.UserException;

@RestControllerAdvice
@RequiredArgsConstructor
public class UserExceptionHandler {

	private final ProblemDetailFactory problemDetailFactory;

	@ExceptionHandler(UserException.class)
	public ResponseEntity<ProblemDetail> handleUserException(UserException ex, HttpServletRequest request) {
		UserProblemType problemType = UserProblemType.from(ex.getErrorCode());
		ProblemDetail problemDetail = problemDetailFactory.create(problemType, ex.getMessage(), request.getRequestURI());
		return ResponseEntity.status(problemType.status()).body(problemDetail);
	}
}
