package com.pikume.back.support.adapter.in.web;

import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.support.adapter.in.web.problem.SupportProblemType;
import com.pikume.back.support.application.exception.SupportException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice(basePackages = "com.pikume.back.support")
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SupportExceptionHandler {

	private final ProblemDetailFactory problemDetailFactory;

	@ExceptionHandler(SupportException.class)
	public ResponseEntity<ProblemDetail> handleSupportException(
			SupportException exception,
			HttpServletRequest request
	) {
		SupportProblemType problemType = SupportProblemType.from(exception.getErrorCode());
		ProblemDetail problemDetail = problemDetailFactory.create(
				problemType,
				exception.getMessage(),
				request.getRequestURI());
		return ResponseEntity.status(problemType.status()).body(problemDetail);
	}

	@ExceptionHandler(MissingServletRequestPartException.class)
	public ResponseEntity<ProblemDetail> handleMissingRequestPart(
			MissingServletRequestPartException exception,
			HttpServletRequest request
	) {
		SupportProblemType problemType = SupportProblemType.INVALID_INQUIRY;
		ProblemDetail problemDetail = problemDetailFactory.create(
				problemType,
				"필수 문의 요청 값이 없습니다: " + exception.getRequestPartName(),
				request.getRequestURI());
		return ResponseEntity.status(problemType.status()).body(problemDetail);
	}
}
