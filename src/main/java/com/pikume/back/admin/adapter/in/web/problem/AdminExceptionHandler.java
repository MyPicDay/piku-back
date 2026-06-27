package com.pikume.back.admin.adapter.in.web.problem;

import com.pikume.back.admin.application.exception.AdminAuthenticationStoreException;
import com.pikume.back.admin.application.exception.AdminException;
import com.pikume.back.admin.application.exception.AdminProblem;
import com.pikume.back.admin.application.port.out.AdminSessionTelemetryPort;
import com.pikume.back.admin.domain.exception.AdminDomainException;
import com.pikume.back.global.error.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.pikume.back.admin.adapter.in.web")
@RequiredArgsConstructor
@Slf4j
public class AdminExceptionHandler {

	private final ProblemDetailFactory problemDetailFactory;
	private final AdminSessionTelemetryPort telemetryPort;

	@ExceptionHandler(AdminException.class)
	public ResponseEntity<ProblemDetail> handleAdminException(AdminException exception, HttpServletRequest request) {
		ProblemDetail problemDetail = problemDetailFactory.create(
				exception.problem(),
				exception.getMessage(),
				request.getRequestURI());
		return ResponseEntity.status(exception.problem().status())
				.header(HttpHeaders.CACHE_CONTROL, "no-store")
				.body(problemDetail);
	}

	@ExceptionHandler(AdminDomainException.class)
	public ResponseEntity<ProblemDetail> handleAdminDomainException(AdminDomainException exception, HttpServletRequest request) {
		ProblemDetail problemDetail = problemDetailFactory.create(
				AdminProblem.INVALID_REQUEST,
				exception.getMessage(),
				request.getRequestURI());
		return ResponseEntity.status(AdminProblem.INVALID_REQUEST.status())
				.header(HttpHeaders.CACHE_CONTROL, "no-store")
				.body(problemDetail);
	}

	@ExceptionHandler(AdminAuthenticationStoreException.class)
	public ResponseEntity<ProblemDetail> handleStoreUnavailable(
			AdminAuthenticationStoreException exception, HttpServletRequest request) {
		telemetryPort.sessionStoreUnavailable();
		log.error("event=admin_store_unavailable outcome=failed exception={}",
				exception.getClass().getSimpleName());
		ProblemDetail problemDetail = problemDetailFactory.create(
				AdminProblem.SESSION_STORE_UNAVAILABLE,
				"관리자 인증 저장소를 확인할 수 없습니다.",
				request.getRequestURI());
		return ResponseEntity.status(AdminProblem.SESSION_STORE_UNAVAILABLE.status())
				.header(HttpHeaders.CACHE_CONTROL, "no-store")
				.body(problemDetail);
	}
}
