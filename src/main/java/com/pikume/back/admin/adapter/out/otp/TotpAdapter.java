package com.pikume.back.admin.adapter.out.otp;

import com.pikume.back.admin.application.port.out.AdminOtpPort;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;

@Component
public class TotpAdapter implements AdminOtpPort {

	private static final char[] BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
	private static final long TIME_STEP_SECONDS = 30L;
	private static final int CODE_DIGITS = 6;

	private final SecureRandom secureRandom = new SecureRandom();

	@Override
	public String generateSecret() {
		StringBuilder secret = new StringBuilder(32);
		for (int i = 0; i < 32; i++) {
			secret.append(BASE32[secureRandom.nextInt(BASE32.length)]);
		}
		return secret.toString();
	}

	@Override
	public String provisioningUri(String issuer, String accountName, String secret) {
		String label = encode(issuer + ":" + accountName);
		return "otpauth://totp/" + label
				+ "?secret=" + encode(secret)
				+ "&issuer=" + encode(issuer)
				+ "&digits=" + CODE_DIGITS
				+ "&period=" + TIME_STEP_SECONDS;
	}

	@Override
	public boolean verify(String secret, String code) {
		if (code == null || !code.matches("\\d{6}")) {
			return false;
		}
		long currentWindow = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;
		for (long offset = -1; offset <= 1; offset++) {
			if (generateCode(secret, currentWindow + offset).equals(code)) {
				return true;
			}
		}
		return false;
	}

	private String generateCode(String secret, long window) {
		try {
			byte[] key = decodeBase32(secret);
			byte[] data = ByteBuffer.allocate(Long.BYTES).putLong(window).array();
			Mac mac = Mac.getInstance("HmacSHA1");
			mac.init(new SecretKeySpec(key, "HmacSHA1"));
			byte[] hash = mac.doFinal(data);
			int offset = hash[hash.length - 1] & 0x0F;
			int binary = ((hash[offset] & 0x7F) << 24)
					| ((hash[offset + 1] & 0xFF) << 16)
					| ((hash[offset + 2] & 0xFF) << 8)
					| (hash[offset + 3] & 0xFF);
			int otp = binary % 1_000_000;
			return String.format("%06d", otp);
		} catch (Exception e) {
			throw new IllegalStateException("OTP 코드를 생성할 수 없습니다.", e);
		}
	}

	private byte[] decodeBase32(String secret) {
		int buffer = 0;
		int bitsLeft = 0;
		java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
		for (char c : secret.toUpperCase().toCharArray()) {
			if (c == '=') {
				break;
			}
			int value = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".indexOf(c);
			if (value < 0) {
				throw new IllegalArgumentException("Base32 secret is invalid.");
			}
			buffer = (buffer << 5) | value;
			bitsLeft += 5;
			if (bitsLeft >= 8) {
				output.write((buffer >> (bitsLeft - 8)) & 0xFF);
				bitsLeft -= 8;
			}
		}
		return output.toByteArray();
	}

	private String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
	}
}
