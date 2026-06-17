package com.pikume.back.admin.adapter.in.web;

import com.pikume.back.admin.adapter.in.web.dto.request.CreateAdminAccountRequest;
import com.pikume.back.admin.application.exception.AdminException;
import com.pikume.back.admin.application.exception.AdminProblem;
import com.pikume.back.admin.application.port.in.CreateAdminAccountUseCase;
import com.pikume.back.admin.application.service.CreateAdminAccountCommand;
import com.pikume.back.admin.application.service.CreateAdminAccountResult;
import com.pikume.back.security.config.AdminUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Accounts", description = "관리자 계정 운영 API")
@RestController
@RequestMapping("/api/admin/accounts")
@RequiredArgsConstructor
public class AdminAccountController {

	private final CreateAdminAccountUseCase createAdminAccountUseCase;

	@Operation(summary = "관리자 계정 생성", description = "SUPER_ADMIN이 관리자 계정을 생성하고 임시 패스워드를 한 번만 반환합니다.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "관리자 계정 생성 성공"),
			@ApiResponse(responseCode = "401", description = "관리자 인증 필요"),
			@ApiResponse(responseCode = "403", description = "관리자 생성 권한 없음"),
			@ApiResponse(responseCode = "409", description = "관리자 이메일 중복")
	})
	@PostMapping
	public ResponseEntity<CreateAdminAccountResult> create(
			@AuthenticationPrincipal AdminUserDetails admin,
			@RequestBody CreateAdminAccountRequest request) {
		if (admin == null) {
			throw new AdminException(AdminProblem.UNAUTHENTICATED, "관리자 인증이 필요합니다.");
		}
		CreateAdminAccountResult result = createAdminAccountUseCase.create(new CreateAdminAccountCommand(
				admin.getId(),
				request.email(),
				request.nickname(),
				request.role()));
		return ResponseEntity.status(HttpStatus.CREATED).body(result);
	}
}
