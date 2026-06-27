package com.pikume.back.admin.adapter.in.web;

import com.pikume.back.admin.application.service.AdminSessionCredentials;
import com.pikume.back.admin.application.port.in.AdminSessionSecurityUseCase;
import com.pikume.back.security.config.AdminSessionCookieManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;

@Tag(name = "Admin Auth", description = "관리자 세션 및 CSRF 인증 API")
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminCsrfController {

	private final AdminSessionSecurityUseCase adminSessionSecurityUseCase;
	private final AdminSessionCookieManager cookieManager;

	@Operation(summary = "관리자 CSRF 초기화", description = "권한 없는 사전 세션과 CSRF 쿠키를 발급합니다.")
	@PostMapping("/csrf")
	public ResponseEntity<Void> initialize() {
		AdminSessionCredentials credentials = adminSessionSecurityUseCase.initialize(LocalDateTime.now());
		return ResponseEntity.noContent()
				.header(HttpHeaders.SET_COOKIE,
						cookieManager.sessionCookie(credentials, Duration.ofMinutes(10)).toString(),
						cookieManager.csrfCookie(credentials, Duration.ofMinutes(10)).toString())
				.header(HttpHeaders.CACHE_CONTROL, "no-store")
				.build();
	}
}
