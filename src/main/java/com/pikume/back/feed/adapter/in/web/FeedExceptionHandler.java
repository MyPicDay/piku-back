package com.pikume.back.feed.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.pikume.back.feed.adapter.in.web.problem.FeedProblemType;
import com.pikume.back.feed.application.exception.FeedException;
import com.pikume.back.global.error.ProblemDetailFactory;

@RestControllerAdvice(basePackages = "com.pikume.back.feed")
@RequiredArgsConstructor
public class FeedExceptionHandler {

	private final ProblemDetailFactory problemDetailFactory;

	@ExceptionHandler(FeedException.class)
	public ResponseEntity<ProblemDetail> handleFeedException(FeedException ex, HttpServletRequest request) {
		FeedProblemType problemType = FeedProblemType.from(ex.getErrorCode());
		ProblemDetail problemDetail = problemDetailFactory.create(
				problemType,
				ex.getMessage(),
				request.getRequestURI());
		return ResponseEntity.status(problemType.status()).body(problemDetail);
	}
}
