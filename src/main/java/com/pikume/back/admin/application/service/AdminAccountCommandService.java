package com.pikume.back.admin.application.service;

import com.pikume.back.admin.application.exception.AdminException;
import com.pikume.back.admin.application.exception.AdminErrorCode;
import com.pikume.back.admin.application.port.in.CreateAdminAccountUseCase;
import com.pikume.back.admin.application.port.out.AdminPasswordPort;
import com.pikume.back.admin.application.port.out.GenerateTemporaryPasswordPort;
import com.pikume.back.admin.application.port.out.QueryAdminAccountPort;
import com.pikume.back.admin.application.port.out.AppendAdminAuditLogPort;
import com.pikume.back.admin.application.port.out.RecordAdminAccountPort;
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

	private final QueryAdminAccountPort queryAdminAccountPort;
	private final RecordAdminAccountPort recordAdminAccountPort;
	private final AppendAdminAuditLogPort appendAdminAuditLogPort;
	private final GenerateTemporaryPasswordPort generateTemporaryPasswordPort;
	private final SendAdminGuideEmailPort sendAdminGuideEmailPort;
	private final AdminPasswordPort adminPasswordPort;

	@Override
	@Transactional
	public CreateAdminAccountResult create(CreateAdminAccountCommand command) {
		AdminAccount actor = queryAdminAccountPort.findAccount(command.actorAdminId())
				.orElseThrow(() -> new AdminException(AdminErrorCode.FORBIDDEN, "관리자 계정 생성 권한이 없습니다."));
		if (!actor.isSuperAdmin()) {
			throw new AdminException(AdminErrorCode.FORBIDDEN, "SUPER_ADMIN만 관리자 계정을 생성할 수 있습니다.");
		}

		String email = AdminEmail.normalize(command.email());
		if (queryAdminAccountPort.emailAlreadyRegistered(email)) {
			throw new AdminException(AdminErrorCode.DUPLICATE_EMAIL, "이미 등록된 관리자 이메일입니다.");
		}

		AdminRole role = command.role();
		if (role == null) {
			throw new AdminException(AdminErrorCode.INVALID_REQUEST, "관리자 등급은 필수입니다.");
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

		AdminAccount saved = recordAdminAccountPort.recordAccount(adminAccount);
		boolean guideEmailSent = true;
		try {
			sendAdminGuideEmailPort.sendAccountCreatedGuide(saved.getEmail(), saved.getEmail(), expiresAt);
		} catch (RuntimeException e) {
			guideEmailSent = false;
		}
		appendAdminAuditLogPort.appendAuditLog(AdminAuditLog.record(
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
