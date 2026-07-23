package com.pikume.back.admin.adapter.in.web;

import com.pikume.back.admin.adapter.in.web.dto.request.CreateAdminAccountRequest;
import com.pikume.back.admin.adapter.in.web.dto.request.ChangeAdminEmailRequest;
import com.pikume.back.admin.adapter.in.web.dto.request.ChangeAdminRoleRequest;
import com.pikume.back.admin.adapter.in.web.dto.request.DeactivateAdminAccountRequest;
import com.pikume.back.admin.application.exception.AdminException;
import com.pikume.back.admin.application.exception.AdminErrorCode;
import com.pikume.back.admin.application.port.in.AdminAccountOperationUseCase;
import com.pikume.back.admin.application.port.in.CreateAdminAccountUseCase;
import com.pikume.back.admin.application.service.AdminAccountDetailResult;
import com.pikume.back.admin.application.service.AdminAccountSummaryResult;
import com.pikume.back.admin.application.service.CreateAdminAccountCommand;
import com.pikume.back.admin.application.service.CreateAdminAccountResult;
import com.pikume.back.admin.application.service.AdminTemporaryPasswordResult;
import com.pikume.back.global.dto.MessageResponse;
import com.pikume.back.security.config.AdminUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin Accounts", description = "관리자 계정 운영 API")
@ApiResponses({
		@ApiResponse(responseCode = "400", description = "유효하지 않은 관리자 계정 요청",
				content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
						schema = @Schema(implementation = ProblemDetail.class))),
		@ApiResponse(responseCode = "401", description = "관리자 인증 필요",
				content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
						schema = @Schema(implementation = ProblemDetail.class))),
		@ApiResponse(responseCode = "403", description = "관리자 계정 운영 권한 없음",
				content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
						schema = @Schema(implementation = ProblemDetail.class))),
		@ApiResponse(responseCode = "404", description = "관리자 계정 없음",
				content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
						schema = @Schema(implementation = ProblemDetail.class))),
		@ApiResponse(responseCode = "409", description = "관리자 식별 정보 중복",
				content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
						schema = @Schema(implementation = ProblemDetail.class))),
		@ApiResponse(responseCode = "503", description = "관리자 인증 저장소 확인 불가",
				content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
						schema = @Schema(implementation = ProblemDetail.class)))
})
@RestController
@ConditionalOnProperty(prefix = "admin.api", name = "account-management-enabled", havingValue = "true")
@RequestMapping("/api/admin/accounts")
@RequiredArgsConstructor
public class AdminAccountController {

	private final CreateAdminAccountUseCase createAdminAccountUseCase;
	private final AdminAccountOperationUseCase adminAccountOperationUseCase;

	@Operation(summary = "관리자 목록 조회", description = "관리자 식별값과 마스킹된 이메일 및 로그인 아이디를 포함한 계정 목록을 조회합니다.")
	@GetMapping
	public ResponseEntity<List<AdminAccountSummaryResult>> list(@AuthenticationPrincipal AdminUserDetails admin) {
		return ResponseEntity.ok(adminAccountOperationUseCase.list(requireAdminId(admin)));
	}

	@Operation(summary = "관리자 상세 조회", description = "관리자 식별값으로 상세 정보를 조회하며 이메일과 로그인 아이디는 마스킹해 반환합니다.")
	@GetMapping("/{adminId}")
	public ResponseEntity<AdminAccountDetailResult> detail(
			@AuthenticationPrincipal AdminUserDetails admin,
			@PathVariable String adminId) {
		return ResponseEntity.ok(adminAccountOperationUseCase.detailById(requireAdminId(admin), adminId));
	}

	@Operation(summary = "관리자 계정 생성", description = "SUPER_ADMIN이 관리자 계정을 생성하고 임시 패스워드를 한 번만 반환합니다.")
	@ApiResponse(responseCode = "201", description = "관리자 계정 생성 성공")
	@PostMapping
	public ResponseEntity<CreateAdminAccountResult> create(
			@AuthenticationPrincipal AdminUserDetails admin,
			@RequestBody CreateAdminAccountRequest request) {
		CreateAdminAccountResult result = createAdminAccountUseCase.create(new CreateAdminAccountCommand(
				requireAdminId(admin),
				request.email(),
				request.nickname(),
				request.role()));
		return ResponseEntity.status(HttpStatus.CREATED).body(result);
	}

	@Operation(summary = "관리자 등급 변경", description = "SUPER_ADMIN이 관리자 등급을 변경합니다.")
	@PatchMapping("/{adminId}/role")
	public ResponseEntity<MessageResponse> changeRole(
			@AuthenticationPrincipal AdminUserDetails admin,
			@PathVariable String adminId,
			@RequestBody ChangeAdminRoleRequest request) {
		adminAccountOperationUseCase.changeRole(requireAdminId(admin), adminId, request.role());
		return ResponseEntity.ok(new MessageResponse("관리자 등급 변경 완료"));
	}

	@Operation(summary = "관리자 계정 비활성화", description = "SUPER_ADMIN이 관리자 계정을 비활성화합니다. 사유는 필수입니다.")
	@PatchMapping("/{adminId}/deactivation")
	public ResponseEntity<MessageResponse> deactivate(
			@AuthenticationPrincipal AdminUserDetails admin,
			@PathVariable String adminId,
			@RequestBody DeactivateAdminAccountRequest request) {
		adminAccountOperationUseCase.deactivate(requireAdminId(admin), adminId, request.reason());
		return ResponseEntity.ok(new MessageResponse("관리자 계정 비활성화 완료"));
	}

	@Operation(summary = "관리자 계정 재활성화", description = "SUPER_ADMIN이 관리자 계정을 재활성화합니다.")
	@PatchMapping("/{adminId}/reactivation")
	public ResponseEntity<MessageResponse> reactivate(
			@AuthenticationPrincipal AdminUserDetails admin,
			@PathVariable String adminId) {
		adminAccountOperationUseCase.reactivate(requireAdminId(admin), adminId);
		return ResponseEntity.ok(new MessageResponse("관리자 계정 재활성화 완료"));
	}

	@Operation(summary = "관리자 계정 잠금 해제", description = "SUPER_ADMIN이 잠긴 관리자 계정을 해제합니다.")
	@PatchMapping("/{adminId}/unlock")
	public ResponseEntity<MessageResponse> unlock(
			@AuthenticationPrincipal AdminUserDetails admin,
			@PathVariable String adminId) {
		adminAccountOperationUseCase.unlock(requireAdminId(admin), adminId);
		return ResponseEntity.ok(new MessageResponse("관리자 계정 잠금 해제 완료"));
	}

	@Operation(summary = "관리자 임시 패스워드 재발급", description = "SUPER_ADMIN이 최초 설정 전 관리자 임시 패스워드를 재발급합니다.")
	@PostMapping("/{adminId}/temporary-password")
	public ResponseEntity<AdminTemporaryPasswordResult> reissueTemporaryPassword(
			@AuthenticationPrincipal AdminUserDetails admin,
			@PathVariable String adminId) {
		return ResponseEntity.ok(adminAccountOperationUseCase.reissueTemporaryPassword(requireAdminId(admin), adminId));
	}

	@Operation(summary = "관리자 OTP 초기화", description = "SUPER_ADMIN이 관리자 OTP를 초기화하고 활성 세션을 폐기합니다.")
	@PostMapping("/{adminId}/otp/reset")
	public ResponseEntity<MessageResponse> resetOtp(
			@AuthenticationPrincipal AdminUserDetails admin,
			@PathVariable String adminId) {
		adminAccountOperationUseCase.resetOtp(requireAdminId(admin), adminId);
		return ResponseEntity.ok(new MessageResponse("관리자 OTP 초기화 완료"));
	}

	@Operation(summary = "관리자 이메일 변경", description = "SUPER_ADMIN이 관리자 이메일을 변경합니다.")
	@PatchMapping("/{adminId}/email")
	public ResponseEntity<MessageResponse> changeEmail(
			@AuthenticationPrincipal AdminUserDetails admin,
			@PathVariable String adminId,
			@RequestBody ChangeAdminEmailRequest request) {
		adminAccountOperationUseCase.changeEmail(requireAdminId(admin), adminId, request.email());
		return ResponseEntity.ok(new MessageResponse("관리자 이메일 변경 완료"));
	}

	private String requireAdminId(AdminUserDetails admin) {
		if (admin == null) {
			throw new AdminException(AdminErrorCode.UNAUTHENTICATED, "관리자 인증이 필요합니다.");
		}
		return admin.getId();
	}
}
