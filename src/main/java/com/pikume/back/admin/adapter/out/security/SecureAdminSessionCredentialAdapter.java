package com.pikume.back.admin.adapter.out.security;

import com.pikume.back.admin.application.port.out.AdminSessionCredentialPort;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class SecureAdminSessionCredentialAdapter implements AdminSessionCredentialPort {

	private static final int CREDENTIAL_BYTES = 32;
	private final SecureRandom secureRandom = new SecureRandom();

	@Override
	public String generate() {
		byte[] value = new byte[CREDENTIAL_BYTES];
		secureRandom.nextBytes(value);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
	}

	@Override
	public String hash(String credential) {
		if (credential == null || credential.isBlank()) {
			throw new IllegalArgumentException("관리자 세션 자격 증명은 필수입니다.");
		}
		return HexFormat.of().formatHex(sha256(credential));
	}

	@Override
	public boolean matches(String credential, String expectedHash) {
		if (credential == null || credential.isBlank() || expectedHash == null || expectedHash.isBlank()) {
			return false;
		}
		byte[] actual = hash(credential).getBytes(StandardCharsets.US_ASCII);
		byte[] expected = expectedHash.getBytes(StandardCharsets.US_ASCII);
		return MessageDigest.isEqual(actual, expected);
	}

	private byte[] sha256(String value) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", exception);
		}
	}
}
