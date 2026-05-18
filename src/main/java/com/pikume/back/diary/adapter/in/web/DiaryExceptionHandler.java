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
import com.pikume.back.diary.domain.exception.DiaryAccessDeniedException;
import com.pikume.back.diary.domain.exception.DiaryNotFoundException;
import com.pikume.back.diary.domain.exception.DuplicateDiaryException;
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

	@ExceptionHandler(DiaryAccessDeniedException.class)
	public ResponseEntity<ProblemDetail> handleDiaryAccessDeniedException(DiaryAccessDeniedException ex,
			HttpServletRequest request) {
		return problem(DiaryProblemType.FORBIDDEN, ex.getMessage(), request);
	}

	@ExceptionHandler(DiaryNotFoundException.class)
	public ResponseEntity<ProblemDetail> handleDiaryNotFoundException(DiaryNotFoundException ex,
			HttpServletRequest request) {
		return problem(DiaryProblemType.NOT_FOUND, ex.getMessage(), request);
	}

	@ExceptionHandler(DuplicateDiaryException.class)
	public ResponseEntity<ProblemDetail> handleDuplicateDiaryException(DuplicateDiaryException ex,
			HttpServletRequest request) {
		return problem(DiaryProblemType.CONFLICT, ex.getMessage(), request);
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
