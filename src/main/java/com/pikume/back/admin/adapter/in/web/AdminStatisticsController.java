package com.pikume.back.admin.adapter.in.web;

import com.pikume.back.admin.application.exception.AdminException;
import com.pikume.back.admin.application.exception.AdminProblem;
import com.pikume.back.admin.application.port.in.AdminStatisticsUseCase;
import com.pikume.back.admin.application.service.AdminStatisticsResponse;
import com.pikume.back.security.config.AdminUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "Admin Statistics", description = "관리자 통계 조회 API")
@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
public class AdminStatisticsController {

	private final AdminStatisticsUseCase adminStatisticsUseCase;

	@Operation(summary = "관리자 통계 대시보드 조회", description = "기본 기간은 오늘을 포함한 최근 7일입니다.")
	@GetMapping("/dashboard")
	public ResponseEntity<AdminStatisticsResponse> dashboard(
			@AuthenticationPrincipal AdminUserDetails admin,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
		return ResponseEntity.ok(adminStatisticsUseCase.getStatistics(requireAdminId(admin), startDate, endDate));
	}

	@Operation(summary = "관리자 통계 CSV 조회", description = "화면 조회와 같은 기간 제한을 적용합니다.")
	@GetMapping(value = "/dashboard.csv", produces = "text/csv")
	public ResponseEntity<String> dashboardCsv(
			@AuthenticationPrincipal AdminUserDetails admin,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
		String csv = adminStatisticsUseCase.getStatisticsCsv(requireAdminId(admin), startDate, endDate);
		return ResponseEntity.ok()
				.contentType(new MediaType("text", "csv"))
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"admin-statistics.csv\"")
				.body(csv);
	}

	private String requireAdminId(AdminUserDetails admin) {
		if (admin == null) {
			throw new AdminException(AdminProblem.UNAUTHENTICATED, "관리자 인증이 필요합니다.");
		}
		return admin.getId();
	}
}
