package com.pikume.back.admin.adapter.out.security;

import com.pikume.back.admin.application.port.out.HashAdminRefreshTokenPort;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class Sha256AdminRefreshTokenHasher implements HashAdminRefreshTokenPort {

	@Override
	public String hash(String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			throw new IllegalArgumentException("관리자 Refresh Token은 필수입니다.");
		}
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception e) {
			throw new IllegalStateException("관리자 Refresh Token을 해시할 수 없습니다.", e);
		}
	}
}
