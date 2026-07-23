package com.pikume.back.creative.adapter.in.web;

import com.pikume.back.creative.adapter.in.web.problem.CreativeProblemType;
import com.pikume.back.creative.application.exception.CreativeException;
import com.pikume.back.global.error.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.pikume.back.creative")
@RequiredArgsConstructor
public class CreativeExceptionHandler {

	private final ProblemDetailFactory problemDetailFactory;

	@ExceptionHandler(CreativeException.class)
	public ResponseEntity<ProblemDetail> handleCreativeException(
			CreativeException exception,
			HttpServletRequest request
	) {
		CreativeProblemType problemType = CreativeProblemType.from(exception.getErrorCode());
		ProblemDetail problemDetail = problemDetailFactory.create(
				problemType,
				exception.getMessage(),
				request.getRequestURI());
		return ResponseEntity.status(problemType.status()).body(problemDetail);
	}
}
