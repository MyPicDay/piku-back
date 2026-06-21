package com.pikume.back.admin.application.service;

import com.pikume.back.admin.application.exception.AdminException;
import com.pikume.back.admin.application.exception.AdminProblem;
import com.pikume.back.admin.application.port.in.CreateAdminAccountUseCase;
import com.pikume.back.admin.application.port.out.AdminPasswordPort;
import com.pikume.back.admin.application.port.out.GenerateTemporaryPasswordPort;
import com.pikume.back.admin.application.port.out.LoadAdminAccountPort;
import com.pikume.back.admin.application.port.out.SaveAdminAuditLogPort;
import com.pikume.back.admin.application.port.out.SaveAdminAccountPort;
import com.pikume.back.admin.application.port.out.SendAdminGuideEmailPort;
import com.pikume.back.admin.domain.AdminAccount;
import com.pikume.back.admin.domain.AdminAuditAction;
import com.pikume.back.admin.domain.AdminAuditLog;
import com.pikume.back.admin.domain.AdminEmail;
import com.pikume.back.admin.domain.AdminRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminAccountCommandService implements CreateAdminAccountUseCase {

	private final LoadAdminAccountPort loadAdminAccountPort;
	private final SaveAdminAccountPort saveAdminAccountPort;
	private final SaveAdminAuditLogPort saveAdminAuditLogPort;
	private final GenerateTemporaryPasswordPort generateTemporaryPasswordPort;
	private final SendAdminGuideEmailPort sendAdminGuideEmailPort;
	private final AdminPasswordPort adminPasswordPort;

	@Override
	@Transactional
	public CreateAdminAccountResult create(CreateAdminAccountCommand command) {
		AdminAccount actor = loadAdminAccountPort.findById(command.actorAdminId())
				.orElseThrow(() -> new AdminException(AdminProblem.FORBIDDEN, "관리자 계정 생성 권한이 없습니다."));
		if (!actor.isSuperAdmin()) {
			throw new AdminException(AdminProblem.FORBIDDEN, "SUPER_ADMIN만 관리자 계정을 생성할 수 있습니다.");
		}

		String email = AdminEmail.normalize(command.email());
		if (loadAdminAccountPort.existsByEmail(email)) {
			throw new AdminException(AdminProblem.DUPLICATE_EMAIL, "이미 등록된 관리자 이메일입니다.");
		}

		AdminRole role = command.role();
		if (role == null) {
			throw new AdminException(AdminProblem.INVALID_REQUEST, "관리자 등급은 필수입니다.");
		}

		String temporaryPassword = generateTemporaryPasswordPort.generate();
		LocalDateTime issuedAt = LocalDateTime.now();
		LocalDateTime expiresAt = issuedAt.plusHours(24);
		AdminAccount adminAccount = AdminAccount.invite(
				email,
				command.nickname(),
				role,
				adminPasswordPort.encode(temporaryPassword),
				issuedAt,
				expiresAt);

		AdminAccount saved = saveAdminAccountPort.save(adminAccount);
		boolean guideEmailSent = true;
		try {
			sendAdminGuideEmailPort.sendAccountCreatedGuide(saved.getEmail(), saved.getEmail(), expiresAt);
		} catch (RuntimeException e) {
			guideEmailSent = false;
		}
		saveAdminAuditLogPort.save(AdminAuditLog.record(
				actor.getId(),
				saved.getId(),
				AdminAuditAction.ADMIN_CREATED,
				null,
				"role: %s".formatted(saved.getRole()),
				LocalDateTime.now()));

		return new CreateAdminAccountResult(
				saved.getNickname(),
				saved.getRole(),
				saved.getStatus(),
				temporaryPassword,
				expiresAt,
				guideEmailSent);
	}
}
