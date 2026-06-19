package com.pikume.back.admin.application.service;

import com.pikume.back.admin.application.exception.AdminException;
import com.pikume.back.admin.application.port.out.GenerateTemporaryPasswordPort;
import com.pikume.back.admin.application.port.out.AdminPasswordPort;
import com.pikume.back.admin.application.port.out.LoadAdminAccountPort;
import com.pikume.back.admin.application.port.out.SaveAdminAuditLogPort;
import com.pikume.back.admin.application.port.out.SaveAdminAccountPort;
import com.pikume.back.admin.application.port.out.SendAdminGuideEmailPort;
import com.pikume.back.admin.domain.AdminAccount;
import com.pikume.back.admin.domain.AdminRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAccountCommandService")
class AdminAccountCommandServiceTest {

	@Mock
	private LoadAdminAccountPort loadAdminAccountPort;
	@Mock
	private SaveAdminAccountPort saveAdminAccountPort;
	@Mock
	private SaveAdminAuditLogPort saveAdminAuditLogPort;
	@Mock
	private GenerateTemporaryPasswordPort generateTemporaryPasswordPort;
	@Mock
	private SendAdminGuideEmailPort sendAdminGuideEmailPort;
	@Mock
	private AdminPasswordPort adminPasswordPort;

	@Test
	@DisplayName("SUPER_ADMIN은 임시 패스워드를 한 번만 포함한 관리자 계정 생성 결과를 받는다")
	void superAdminCreatesAdminAccount() {
		AdminAccount actor = admin(AdminRole.SUPER_ADMIN, "super@pikume.com");
		given(loadAdminAccountPort.findById(actor.getId())).willReturn(Optional.of(actor));
		given(loadAdminAccountPort.existsByEmail("viewer@pikume.com")).willReturn(false);
		given(generateTemporaryPasswordPort.generate()).willReturn("TempPass1!234567");
		given(adminPasswordPort.encode("TempPass1!234567")).willReturn("encoded-temp");
		given(saveAdminAccountPort.save(any(AdminAccount.class))).willAnswer(invocation -> invocation.getArgument(0));
		AdminAccountCommandService service = service();

		CreateAdminAccountResult result = service.create(new CreateAdminAccountCommand(
				actor.getId(),
				"Viewer@Pikume.com",
				"조회자1",
				AdminRole.VIEWER));

		ArgumentCaptor<AdminAccount> savedCaptor = ArgumentCaptor.forClass(AdminAccount.class);
		then(saveAdminAccountPort).should().save(savedCaptor.capture());
		assertThat(savedCaptor.getValue().getEmail()).isEqualTo("viewer@pikume.com");
		assertThat(savedCaptor.getValue().getTemporaryPasswordHash()).isEqualTo("encoded-temp");
		assertThat(result.temporaryLoginId()).isEqualTo("viewer@pikume.com");
		assertThat(result.temporaryPassword()).isEqualTo("TempPass1!234567");
		assertThat(result.guideEmailSent()).isTrue();
		then(sendAdminGuideEmailPort).should()
				.sendAccountCreatedGuide("viewer@pikume.com", "viewer@pikume.com", result.temporaryCredentialExpiresAt());
	}

	@Test
	@DisplayName("SUPER_ADMIN이 아니면 관리자 계정을 생성할 수 없다")
	void nonSuperAdminCannotCreateAdminAccount() {
		AdminAccount actor = admin(AdminRole.OPERATOR, "operator@pikume.com");
		given(loadAdminAccountPort.findById(actor.getId())).willReturn(Optional.of(actor));
		AdminAccountCommandService service = service();

		assertThatThrownBy(() -> service.create(new CreateAdminAccountCommand(
				actor.getId(),
				"viewer@pikume.com",
				"조회자1",
				AdminRole.VIEWER)))
				.isInstanceOf(AdminException.class);
	}

	@Test
	@DisplayName("중복 이메일로 관리자 계정을 생성할 수 없다")
	void duplicateEmailCannotBeCreated() {
		AdminAccount actor = admin(AdminRole.SUPER_ADMIN, "super@pikume.com");
		given(loadAdminAccountPort.findById(actor.getId())).willReturn(Optional.of(actor));
		given(loadAdminAccountPort.existsByEmail("viewer@pikume.com")).willReturn(true);
		AdminAccountCommandService service = service();

		assertThatThrownBy(() -> service.create(new CreateAdminAccountCommand(
				actor.getId(),
				"viewer@pikume.com",
				"조회자1",
				AdminRole.VIEWER)))
				.isInstanceOf(AdminException.class);
	}

	private AdminAccountCommandService service() {
		return new AdminAccountCommandService(
				loadAdminAccountPort,
				saveAdminAccountPort,
				saveAdminAuditLogPort,
				generateTemporaryPasswordPort,
				sendAdminGuideEmailPort,
				adminPasswordPort);
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
