package com.pikume.back.admin.application.port.in;

import com.pikume.back.admin.application.service.AdminDashboardResponse;

public interface AdminDashboardUseCase {

	AdminDashboardResponse getDashboard(String actorAdminId);
}
