package com.pikume.back.admin.application.port.out;

public interface HashAdminRefreshTokenPort {

	String hash(String refreshToken);
}
