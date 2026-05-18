package com.pikume.back.feed.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.pikume.back.feed.adapter.in.web.problem.FeedProblemType;
import com.pikume.back.feed.domain.exception.FeedDiaryNotFoundException;
import com.pikume.back.feed.domain.exception.InvalidFeedCursorException;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.global.error.ValidationProblemType;

@RestControllerAdvice(basePackages = "com.pikume.back.feed")
@RequiredArgsConstructor
public class FeedExceptionHandler {

	private final ProblemDetailFactory problemDetailFactory;

	@ExceptionHandler(FeedDiaryNotFoundException.class)
	public ResponseEntity<ProblemDetail> handleFeedDiaryNotFoundException(HttpServletRequest request) {
		ProblemDetail problemDetail = problemDetailFactory.create(
				FeedProblemType.DIARY_NOT_FOUND,
				"일기를 찾을 수 없습니다.",
				request.getRequestURI());
		return ResponseEntity.status(FeedProblemType.DIARY_NOT_FOUND.status()).body(problemDetail);
	}

	@ExceptionHandler(InvalidFeedCursorException.class)
	public ResponseEntity<ProblemDetail> handleInvalidFeedCursorException(InvalidFeedCursorException ex,
			HttpServletRequest request) {
		ProblemDetail problemDetail = problemDetailFactory.create(
				ValidationProblemType.INVALID_REQUEST,
				ex.getMessage(),
				request.getRequestURI());
		return ResponseEntity.status(ValidationProblemType.INVALID_REQUEST.status()).body(problemDetail);
	}
}
