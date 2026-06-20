package com.pikume.back.admin.adapter.out.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * {@code admin.otp} 설정을 관리자 OTP 암호화에 사용할 키로 변환한다.
 *
 * <p>{@code encryption-key}는 Base64로 인코딩된 32바이트 값이어야 한다.
 * 값이 없거나 형식 또는 길이가 올바르지 않으면 애플리케이션 시작을 중단한다.</p>
 */
@ConfigurationProperties(prefix = "admin.otp")
public final class AdminOtpCryptoProperties {

	private static final int KEY_LENGTH_BYTES = 32;
	private final SecretKey secretKey;

	/**
	 * 설정에서 전달된 문자열을 검증하고 AES 키로 변환한다.
	 *
	 * @param encryptionKey Base64로 인코딩된 32바이트 AES 키
	 */
	public AdminOtpCryptoProperties(String encryptionKey) {
		this.secretKey = new SecretKeySpec(decodeAndValidate(encryptionKey), "AES");
	}

	/**
	 * 검증과 변환이 완료된 관리자 OTP 암호화 키를 반환한다.
	 *
	 * @return AES-GCM 암복호화에 사용할 AES 키
	 */
	public SecretKey secretKey() {
		return secretKey;
	}

	private static byte[] decodeAndValidate(String encryptionKey) {
		if (encryptionKey == null || encryptionKey.isBlank()) {
			throw new IllegalArgumentException("관리자 OTP 암호화 키는 필수입니다.");
		}
		byte[] decoded;
		try {
			decoded = Base64.getDecoder().decode(encryptionKey);
		} catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException("관리자 OTP 암호화 키는 Base64 형식이어야 합니다.", exception);
		}
		if (decoded.length != KEY_LENGTH_BYTES) {
			throw new IllegalArgumentException("관리자 OTP 암호화 키는 32바이트여야 합니다.");
		}
		return decoded;
	}
}
