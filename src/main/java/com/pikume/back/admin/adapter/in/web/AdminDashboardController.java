package com.pikume.back.admin.adapter.in.web;

import com.pikume.back.admin.application.exception.AdminException;
import com.pikume.back.admin.application.exception.AdminProblem;
import com.pikume.back.admin.application.port.in.AdminDashboardUseCase;
import com.pikume.back.admin.application.service.AdminDashboardResponse;
import com.pikume.back.security.config.AdminUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Dashboard", description = "관리자 통합 대시보드 API")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

	private final AdminDashboardUseCase adminDashboardUseCase;

	@Operation(summary = "관리자 통합 대시보드 조회")
	@GetMapping("/dashboard")
	public ResponseEntity<AdminDashboardResponse> dashboard(
			@AuthenticationPrincipal AdminUserDetails admin) {
		if (admin == null) {
			throw new AdminException(AdminProblem.UNAUTHENTICATED, "관리자 인증이 필요합니다.");
		}
		return ResponseEntity.ok()
				.header(HttpHeaders.CACHE_CONTROL, "no-store")
				.body(adminDashboardUseCase.getDashboard(admin.getId()));
	}
}
