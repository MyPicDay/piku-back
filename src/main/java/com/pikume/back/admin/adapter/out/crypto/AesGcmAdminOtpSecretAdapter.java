package com.pikume.back.admin.adapter.out.crypto;

import com.pikume.back.admin.application.port.out.ProtectAdminOtpSecretPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AesGcmAdminOtpSecretAdapter implements ProtectAdminOtpSecretPort {

	private static final int IV_LENGTH_BYTES = 12;
	private static final int TAG_LENGTH_BITS = 128;

	private final SecureRandom secureRandom = new SecureRandom();

	@Value("${admin.otp.encryption-key:}")
	private String encryptionKey;

	@Override
	public String protect(String plainSecret) {
		try {
			byte[] iv = new byte[IV_LENGTH_BYTES];
			secureRandom.nextBytes(iv);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
			byte[] encrypted = cipher.doFinal(plainSecret.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length)
					.put(iv)
					.put(encrypted)
					.array());
		} catch (Exception e) {
			throw new IllegalStateException("관리자 OTP 비밀키를 암호화할 수 없습니다.", e);
		}
	}

	@Override
	public String reveal(String protectedSecret) {
		try {
			byte[] decoded = Base64.getDecoder().decode(protectedSecret);
			ByteBuffer buffer = ByteBuffer.wrap(decoded);
			byte[] iv = new byte[IV_LENGTH_BYTES];
			buffer.get(iv);
			byte[] encrypted = new byte[buffer.remaining()];
			buffer.get(encrypted);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
			return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new IllegalStateException("관리자 OTP 비밀키를 복호화할 수 없습니다.", e);
		}
	}

	private SecretKeySpec key() {
		if (encryptionKey == null || encryptionKey.isBlank()) {
			throw new IllegalStateException("관리자 OTP 암호화 키가 설정되지 않았습니다.");
		}
		byte[] decoded = Base64.getDecoder().decode(encryptionKey);
		if (decoded.length != 32) {
			throw new IllegalStateException("관리자 OTP 암호화 키는 32바이트여야 합니다.");
		}
		return new SecretKeySpec(decoded, "AES");
	}
}
