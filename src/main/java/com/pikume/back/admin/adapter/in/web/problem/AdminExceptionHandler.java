package com.pikume.back.admin.adapter.in.web.problem;

import com.pikume.back.admin.application.exception.AdminException;
import com.pikume.back.admin.application.exception.AdminProblem;
import com.pikume.back.admin.domain.exception.AdminDomainException;
import com.pikume.back.global.error.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.pikume.back.admin.adapter.in.web")
@RequiredArgsConstructor
public class AdminExceptionHandler {

	private final ProblemDetailFactory problemDetailFactory;

	@ExceptionHandler(AdminException.class)
	public ResponseEntity<ProblemDetail> handleAdminException(AdminException exception, HttpServletRequest request) {
		ProblemDetail problemDetail = problemDetailFactory.create(
				exception.problem(),
				exception.getMessage(),
				request.getRequestURI());
		return ResponseEntity.status(exception.problem().status()).body(problemDetail);
	}

	@ExceptionHandler(AdminDomainException.class)
	public ResponseEntity<ProblemDetail> handleAdminDomainException(AdminDomainException exception, HttpServletRequest request) {
		ProblemDetail problemDetail = problemDetailFactory.create(
				AdminProblem.INVALID_REQUEST,
				exception.getMessage(),
				request.getRequestURI());
		return ResponseEntity.status(AdminProblem.INVALID_REQUEST.status()).body(problemDetail);
	}
}
