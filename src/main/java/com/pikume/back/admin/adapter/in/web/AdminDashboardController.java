package com.pikume.back.admin.adapter.in.web;

import com.pikume.back.admin.application.exception.AdminException;
import com.pikume.back.admin.application.exception.AdminErrorCode;
import com.pikume.back.admin.application.port.in.AdminDashboardUseCase;
import com.pikume.back.admin.application.service.AdminDashboardResponse;
import com.pikume.back.security.principal.AdminPrincipal;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Dashboard", description = "관리자 통합 대시보드 API")
@ApiResponses({
		@ApiResponse(responseCode = "401", description = "관리자 인증 필요",
				content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
						schema = @Schema(implementation = ProblemDetail.class))),
		@ApiResponse(responseCode = "403", description = "관리자 대시보드 접근 권한 없음",
				content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
						schema = @Schema(implementation = ProblemDetail.class))),
		@ApiResponse(responseCode = "503", description = "관리자 인증 저장소 확인 불가",
				content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
						schema = @Schema(implementation = ProblemDetail.class)))
})
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

	private final AdminDashboardUseCase adminDashboardUseCase;

	@Operation(summary = "관리자 통합 대시보드 조회")
	@GetMapping("/dashboard")
	public ResponseEntity<AdminDashboardResponse> dashboard(
			@AuthenticationPrincipal AdminPrincipal admin) {
		if (admin == null) {
			throw new AdminException(AdminErrorCode.UNAUTHENTICATED, "관리자 인증이 필요합니다.");
		}
		return ResponseEntity.ok()
				.header(HttpHeaders.CACHE_CONTROL, "no-store")
				.body(adminDashboardUseCase.getDashboard(admin.getId()));
	}
}
