package com.pikume.back.admin.application.port.in;

import com.pikume.back.admin.application.service.AdminAccountDetailResult;
import com.pikume.back.admin.application.service.AdminAccountSummaryResult;
import com.pikume.back.admin.application.service.AdminAuditLogResult;
import com.pikume.back.admin.application.service.AdminTemporaryPasswordResult;
import com.pikume.back.admin.domain.AdminRole;

import java.util.List;

public interface AdminAccountOperationUseCase {

	List<AdminAccountSummaryResult> list(String actorAdminId);

	AdminAccountDetailResult detailByEmail(String actorAdminId, String email);

	void changeRole(String actorAdminId, String targetAdminId, AdminRole role);

	void deactivate(String actorAdminId, String targetAdminId, String reason);

	void reactivate(String actorAdminId, String targetAdminId);

	void unlock(String actorAdminId, String targetAdminId);

	AdminTemporaryPasswordResult reissueTemporaryPassword(String actorAdminId, String targetAdminId);

	void resetOtp(String actorAdminId, String targetAdminId);

	void changeEmail(String actorAdminId, String targetAdminId, String newEmail);

	List<AdminAuditLogResult> auditLogs(String actorAdminId, int limit);
}
