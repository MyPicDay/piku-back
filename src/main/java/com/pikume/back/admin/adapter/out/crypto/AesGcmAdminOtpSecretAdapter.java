package com.pikume.back.admin.adapter.out.crypto;

import com.pikume.back.admin.application.port.out.ProtectAdminOtpSecretPort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

import static com.pikume.back.admin.adapter.out.crypto.AdminOtpCryptoConfiguration.ADMIN_OTP_ENCRYPTION_KEY_BEAN;

/**
 * 관리자 OTP 비밀키를 AES-GCM으로 암복호화하는 출력 어댑터다.
 *
 * <p>암호화할 때마다 12바이트 IV를 새로 생성하며, 서버 재시작 후에도 복호화할 수 있도록
 * {@code Base64(IV + 암호문 + 인증 태그)} 형식으로 함께 저장한다.</p>
 */
@Component
public class AesGcmAdminOtpSecretAdapter implements ProtectAdminOtpSecretPort {

	private static final int IV_LENGTH_BYTES = 12;
	private static final int TAG_LENGTH_BITS = 128;

	private final SecureRandom secureRandom = new SecureRandom();
	private final SecretKey encryptionKey;

	/**
	 * 시작 시 검증된 관리자 OTP 전용 AES 키를 주입받는다.
	 *
	 * @param encryptionKey 관리자 OTP 비밀키 보호에 사용할 AES 키
	 */
	public AesGcmAdminOtpSecretAdapter(
			@Qualifier(ADMIN_OTP_ENCRYPTION_KEY_BEAN) SecretKey encryptionKey) {
		this.encryptionKey = Objects.requireNonNull(encryptionKey);
	}

	@Override
	public String protect(String plainSecret) {
		try {
			byte[] iv = new byte[IV_LENGTH_BYTES];
			secureRandom.nextBytes(iv);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
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
			cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
			return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new IllegalStateException("관리자 OTP 비밀키를 복호화할 수 없습니다.", e);
		}
	}

}
