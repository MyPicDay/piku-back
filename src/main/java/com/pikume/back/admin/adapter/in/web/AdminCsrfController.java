package com.pikume.back.admin.adapter.in.web;

import com.pikume.back.admin.application.dto.AdminSessionCredentialResult;
import com.pikume.back.admin.application.port.in.AdminSessionSecurityUseCase;
import com.pikume.back.security.config.AdminSessionCookieManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;

@Tag(name = "Admin Auth", description = "관리자 세션 및 CSRF 인증 API")
@ApiResponses({
		@ApiResponse(responseCode = "403", description = "허용되지 않은 관리자 Origin",
				content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
						schema = @Schema(implementation = ProblemDetail.class))),
		@ApiResponse(responseCode = "503", description = "관리자 인증 저장소 확인 불가",
				content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
						schema = @Schema(implementation = ProblemDetail.class)))
})
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminCsrfController {

	private final AdminSessionSecurityUseCase adminSessionSecurityUseCase;
	private final AdminSessionCookieManager cookieManager;

	@Operation(summary = "관리자 CSRF 초기화", description = "권한 없는 사전 세션과 CSRF 쿠키를 발급합니다.")
	@ApiResponse(responseCode = "204", description = "사전 세션과 CSRF 쿠키 발급 성공")
	@PostMapping("/csrf")
	public ResponseEntity<Void> initialize() {
		AdminSessionCredentialResult credentials = adminSessionSecurityUseCase.initialize(LocalDateTime.now());
		return ResponseEntity.noContent()
				.header(HttpHeaders.SET_COOKIE,
						cookieManager.sessionCookie(credentials, Duration.ofMinutes(10)).toString(),
						cookieManager.csrfCookie(credentials, Duration.ofMinutes(10)).toString())
				.header(HttpHeaders.CACHE_CONTROL, "no-store")
				.build();
	}
}
