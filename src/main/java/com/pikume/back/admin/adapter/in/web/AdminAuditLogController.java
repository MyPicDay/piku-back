package com.pikume.back.admin.adapter.in.web;

import com.pikume.back.admin.application.exception.AdminException;
import com.pikume.back.admin.application.exception.AdminProblem;
import com.pikume.back.admin.application.port.in.AdminAccountOperationUseCase;
import com.pikume.back.admin.application.service.AdminAuditLogResult;
import com.pikume.back.security.config.AdminUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin Audit Logs", description = "관리자 작업 감사 로그 API")
@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
public class AdminAuditLogController {

	private final AdminAccountOperationUseCase adminAccountOperationUseCase;

	@Operation(summary = "관리자 작업 감사 로그 조회", description = "최신 관리자 작업 감사 로그를 조회합니다.")
	@GetMapping
	public ResponseEntity<List<AdminAuditLogResult>> list(
			@AuthenticationPrincipal AdminUserDetails admin,
			@RequestParam(defaultValue = "50") int limit) {
		if (admin == null) {
			throw new AdminException(AdminProblem.UNAUTHENTICATED, "관리자 인증이 필요합니다.");
		}
		return ResponseEntity.ok(adminAccountOperationUseCase.auditLogs(admin.getId(), limit));
	}
}
