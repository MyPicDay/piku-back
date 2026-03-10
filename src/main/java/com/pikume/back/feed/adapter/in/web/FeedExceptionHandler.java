package com.pikume.back.feed.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.pikume.back.feed.domain.exception.FeedDiaryNotFoundException;

@RestControllerAdvice(basePackages = "com.pikume.back.feed")
public class FeedExceptionHandler {

	@ExceptionHandler(FeedDiaryNotFoundException.class)
	public ResponseEntity<FeedErrorResponse> handleFeedDiaryNotFoundException(HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new FeedErrorResponse(
						HttpStatus.NOT_FOUND.value(),
						"일기를 찾을 수 없습니다.",
						request.getRequestURI()));
	}

	public record FeedErrorResponse(
			int status,
			String message,
			String path
	) {
	}
}
