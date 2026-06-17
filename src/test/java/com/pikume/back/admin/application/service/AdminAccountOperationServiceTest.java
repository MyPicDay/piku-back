package com.pikume.back.admin.application.service;

import com.pikume.back.admin.application.exception.AdminException;
import com.pikume.back.admin.application.exception.AdminProblem;
import com.pikume.back.admin.application.port.out.GenerateTemporaryPasswordPort;
import com.pikume.back.admin.application.port.out.LoadAdminAccountPort;
import com.pikume.back.admin.application.port.out.LoadAdminAuditLogPort;
import com.pikume.back.admin.application.port.out.SaveAdminAccountPort;
import com.pikume.back.admin.application.port.out.SaveAdminAuditLogPort;
import com.pikume.back.admin.application.port.out.SearchAdminAccountsPort;
import com.pikume.back.admin.application.port.out.SendAdminGuideEmailPort;
import com.pikume.back.admin.domain.AdminAccount;
import com.pikume.back.admin.domain.AdminAccountStatus;
import com.pikume.back.admin.domain.AdminAuditAction;
import com.pikume.back.admin.domain.AdminAuditLog;
import com.pikume.back.admin.domain.AdminRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAccountOperationService")
class AdminAccountOperationServiceTest {

	@Mock
	private LoadAdminAccountPort loadAdminAccountPort;
	@Mock
	private SaveAdminAccountPort saveAdminAccountPort;
	@Mock
	private SearchAdminAccountsPort searchAdminAccountsPort;
	@Mock
	private SaveAdminAuditLogPort saveAdminAuditLogPort;
	@Mock
	private LoadAdminAuditLogPort loadAdminAuditLogPort;
	@Mock
	private GenerateTemporaryPasswordPort generateTemporaryPasswordPort;
	@Mock
	private SendAdminGuideEmailPort sendAdminGuideEmailPort;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private AdminSessionTokenService adminSessionTokenService;

	@Test
	@DisplayName("관리자 목록 조회는 UUID를 포함하지 않는 요약 결과를 반환한다")
	void listReturnsSummariesWithoutAdminId() {
		AdminAccount actor = admin(AdminRole.SUPER_ADMIN, "super@pikume.com");
		AdminAccount target = admin(AdminRole.VIEWER, "viewer@pikume.com");
		given(loadAdminAccountPort.findById(actor.getId())).willReturn(Optional.of(actor));
		given(searchAdminAccountsPort.findAll()).willReturn(List.of(target));

		List<AdminAccountSummaryResult> result = service().list(actor.getId());

		assertThat(result).hasSize(1);
		assertThat(result.get(0).email()).isEqualTo("viewer@pikume.com");
	}

	@Test
	@DisplayName("상세 조회는 SUPER_ADMIN에게만 관리자 UUID를 포함해 반환한다")
	void detailReturnsAdminIdForSuperAdmin() {
		AdminAccount actor = admin(AdminRole.SUPER_ADMIN, "super@pikume.com");
		AdminAccount target = admin(AdminRole.OPERATOR, "operator@pikume.com");
		given(loadAdminAccountPort.findById(actor.getId())).willReturn(Optional.of(actor));
		given(loadAdminAccountPort.findByEmail("operator@pikume.com")).willReturn(Optional.of(target));

		AdminAccountDetailResult result = service().detailByEmail(actor.getId(), "Operator@Pikume.com");

		assertThat(result.adminId()).isEqualTo(target.getId());
		assertThat(result.email()).isEqualTo("operator@pikume.com");
	}

	@Test
	@DisplayName("SUPER_ADMIN이 아니면 운영 기능을 사용할 수 없다")
	void nonSuperAdminCannotOperateAccounts() {
		AdminAccount actor = admin(AdminRole.OPERATOR, "operator@pikume.com");
		given(loadAdminAccountPort.findById(actor.getId())).willReturn(Optional.of(actor));

		assertThatThrownBy(() -> service().list(actor.getId()))
				.isInstanceOfSatisfying(AdminException.class, exception ->
						assertThat(exception.problem()).isEqualTo(AdminProblem.FORBIDDEN));
	}

	@Test
	@DisplayName("비활성화 사유는 필수이며 성공 시 세션을 폐기하고 감사 로그를 남긴다")
	void deactivateRequiresReasonAndWritesAuditLog() {
		AdminAccount actor = admin(AdminRole.SUPER_ADMIN, "super@pikume.com");
		AdminAccount target = admin(AdminRole.OPERATOR, "operator@pikume.com");
		given(loadAdminAccountPort.findById(actor.getId())).willReturn(Optional.of(actor));
		given(loadAdminAccountPort.findById(target.getId())).willReturn(Optional.of(target));
		given(loadAdminAccountPort.countByRoleAndStatus(AdminRole.SUPER_ADMIN, AdminAccountStatus.ACTIVE))
				.willReturn(1L);
		AdminAccountOperationService service = service();

		assertThatThrownBy(() -> service.deactivate(actor.getId(), target.getId(), " "))
				.isInstanceOfSatisfying(AdminException.class, exception ->
						assertThat(exception.problem()).isEqualTo(AdminProblem.INVALID_REQUEST));

		service.deactivate(actor.getId(), target.getId(), "퇴사");

		ArgumentCaptor<AdminAuditLog> auditCaptor = ArgumentCaptor.forClass(AdminAuditLog.class);
		assertThat(target.getStatus()).isEqualTo(AdminAccountStatus.INACTIVE);
		then(adminSessionTokenService).should().revokeActiveSessions(eq(target.getId()), any(LocalDateTime.class));
		then(saveAdminAuditLogPort).should().save(auditCaptor.capture());
		assertThat(auditCaptor.getValue().getAction()).isEqualTo(AdminAuditAction.DEACTIVATED);
		assertThat(auditCaptor.getValue().getReason()).isEqualTo("퇴사");
	}

	@Test
	@DisplayName("관리자 등급 변경 시 기존 세션을 폐기한다")
	void changeRoleRevokesActiveSessions() {
		AdminAccount actor = admin(AdminRole.SUPER_ADMIN, "super@pikume.com");
		AdminAccount target = admin(AdminRole.OPERATOR, "operator@pikume.com");
		given(loadAdminAccountPort.findById(actor.getId())).willReturn(Optional.of(actor));
		given(loadAdminAccountPort.findById(target.getId())).willReturn(Optional.of(target));
		given(loadAdminAccountPort.countByRoleAndStatus(AdminRole.SUPER_ADMIN, AdminAccountStatus.ACTIVE))
				.willReturn(1L);

		service().changeRole(actor.getId(), target.getId(), AdminRole.VIEWER);

		assertThat(target.getRole()).isEqualTo(AdminRole.VIEWER);
		then(adminSessionTokenService).should().revokeActiveSessions(eq(target.getId()), any(LocalDateTime.class));
	}

	@Test
	@DisplayName("자기 계정은 비활성화하거나 등급을 변경할 수 없다")
	void cannotOperateOwnAdminAccount() {
		AdminAccount actor = admin(AdminRole.SUPER_ADMIN, "super@pikume.com");
		given(loadAdminAccountPort.findById(actor.getId())).willReturn(Optional.of(actor));
		given(loadAdminAccountPort.countByRoleAndStatus(AdminRole.SUPER_ADMIN, AdminAccountStatus.ACTIVE))
				.willReturn(2L);
		AdminAccountOperationService service = service();

		assertThatThrownBy(() -> service.deactivate(actor.getId(), actor.getId(), "퇴사"))
				.isInstanceOfSatisfying(AdminException.class, exception ->
						assertThat(exception.problem()).isEqualTo(AdminProblem.FORBIDDEN));
		assertThatThrownBy(() -> service.changeRole(actor.getId(), actor.getId(), AdminRole.OPERATOR))
				.isInstanceOfSatisfying(AdminException.class, exception ->
						assertThat(exception.problem()).isEqualTo(AdminProblem.FORBIDDEN));
	}

	@Test
	@DisplayName("마지막 SUPER_ADMIN은 비활성화하거나 등급을 낮출 수 없다")
	void cannotRemoveLastSuperAdmin() {
		AdminAccount actor = admin(AdminRole.SUPER_ADMIN, "actor@pikume.com");
		AdminAccount target = admin(AdminRole.SUPER_ADMIN, "target@pikume.com");
		given(loadAdminAccountPort.findById(actor.getId())).willReturn(Optional.of(actor));
		given(loadAdminAccountPort.findById(target.getId())).willReturn(Optional.of(target));
		given(loadAdminAccountPort.countByRoleAndStatus(AdminRole.SUPER_ADMIN, AdminAccountStatus.ACTIVE))
				.willReturn(1L);
		AdminAccountOperationService service = service();

		assertThatThrownBy(() -> service.deactivate(actor.getId(), target.getId(), "퇴사"))
				.isInstanceOfSatisfying(AdminException.class, exception ->
						assertThat(exception.problem()).isEqualTo(AdminProblem.FORBIDDEN));
		assertThatThrownBy(() -> service.changeRole(actor.getId(), target.getId(), AdminRole.OPERATOR))
				.isInstanceOfSatisfying(AdminException.class, exception ->
						assertThat(exception.problem()).isEqualTo(AdminProblem.FORBIDDEN));
	}

	@Test
	@DisplayName("정식 로그인 아이디 설정 전 계정은 임시 패스워드를 재발급할 수 있다")
	void reissueTemporaryPasswordReturnsPasswordOnce() {
		AdminAccount actor = admin(AdminRole.SUPER_ADMIN, "super@pikume.com");
		AdminAccount target = admin(AdminRole.OPERATOR, "operator@pikume.com");
		given(loadAdminAccountPort.findById(actor.getId())).willReturn(Optional.of(actor));
		given(loadAdminAccountPort.findById(target.getId())).willReturn(Optional.of(target));
		given(generateTemporaryPasswordPort.generate()).willReturn("TempPass1!234567");
		given(passwordEncoder.encode("TempPass1!234567")).willReturn("encoded-temp");

		AdminTemporaryPasswordResult result = service().reissueTemporaryPassword(actor.getId(), target.getId());

		assertThat(result.temporaryLoginId()).isEqualTo("operator@pikume.com");
		assertThat(result.temporaryPassword()).isEqualTo("TempPass1!234567");
		assertThat(target.getTemporaryPasswordHash()).isEqualTo("encoded-temp");
	}

	private AdminAccountOperationService service() {
		return new AdminAccountOperationService(
				loadAdminAccountPort,
				saveAdminAccountPort,
				searchAdminAccountsPort,
				saveAdminAuditLogPort,
				loadAdminAuditLogPort,
				generateTemporaryPasswordPort,
				sendAdminGuideEmailPort,
				passwordEncoder,
				adminSessionTokenService);
	}

	private AdminAccount admin(AdminRole role, String email) {
		return AdminAccount.invite(
				email,
				"관리자1",
				role,
				"temp-hash",
				LocalDateTime.of(2026, 6, 17, 13, 0),
				LocalDateTime.of(2026, 6, 18, 13, 0));
	}
}
