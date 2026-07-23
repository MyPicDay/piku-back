package com.pikume.back.character.adapter.in.web;

import com.pikume.back.character.adapter.in.web.problem.CharacterProblemType;
import com.pikume.back.character.application.exception.CharacterException;
import com.pikume.back.global.error.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.pikume.back.character")
@RequiredArgsConstructor
public class CharacterExceptionHandler {

	private final ProblemDetailFactory problemDetailFactory;

	@ExceptionHandler(CharacterException.class)
	public ResponseEntity<ProblemDetail> handleCharacterException(
			CharacterException exception,
			HttpServletRequest request
	) {
		CharacterProblemType problemType = CharacterProblemType.from(exception.getErrorCode());
		ProblemDetail problemDetail = problemDetailFactory.create(
				problemType,
				exception.getMessage(),
				request.getRequestURI());
		return ResponseEntity.status(problemType.status()).body(problemDetail);
	}
}
