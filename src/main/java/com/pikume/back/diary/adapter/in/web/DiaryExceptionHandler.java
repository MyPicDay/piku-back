package com.pikume.back.diary.adapter.in.web;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.pikume.back.diary.adapter.in.web.problem.DiaryProblemType;
import com.pikume.back.diary.application.exception.DiaryException;
import com.pikume.back.global.error.ProblemDetailFactory;

@RestControllerAdvice(basePackages = { "com.pikume.back.diary", "com.pikume.back.comment" })
@RequiredArgsConstructor
public class DiaryExceptionHandler {

	private final ProblemDetailFactory problemDetailFactory;

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ProblemDetail> handleAccessDeniedException(AccessDeniedException ex,
			HttpServletRequest request) {
		return problem(DiaryProblemType.FORBIDDEN, "권한이 없습니다.", request);
	}

	@ExceptionHandler(DiaryException.class)
	public ResponseEntity<ProblemDetail> handleDiaryException(DiaryException ex, HttpServletRequest request) {
		return problem(DiaryProblemType.from(ex.getErrorCode()), ex.getMessage(), request);
	}

	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<ProblemDetail> handleEntityNotFound(EntityNotFoundException ex, HttpServletRequest request) {
		return problem(DiaryProblemType.NOT_FOUND, "엔티티를 찾을 수 없습니다.", request);
	}

	private ResponseEntity<ProblemDetail> problem(DiaryProblemType problemType, String detail, HttpServletRequest request) {
		ProblemDetail problemDetail = problemDetailFactory.create(problemType, detail, request.getRequestURI());
		return ResponseEntity.status(problemType.status()).body(problemDetail);
	}
}
