package com.pikume.back.admin.domain;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

public final class AdminId {

	private static final SecureRandom RANDOM = new SecureRandom();

	private AdminId() {
	}

	public static String newId() {
		byte[] bytes = new byte[16];
		RANDOM.nextBytes(bytes);

		long timestamp = Instant.now().toEpochMilli();
		bytes[0] = (byte)(timestamp >>> 40);
		bytes[1] = (byte)(timestamp >>> 32);
		bytes[2] = (byte)(timestamp >>> 24);
		bytes[3] = (byte)(timestamp >>> 16);
		bytes[4] = (byte)(timestamp >>> 8);
		bytes[5] = (byte)timestamp;
		bytes[6] = (byte)((bytes[6] & 0x0F) | 0x70);
		bytes[8] = (byte)((bytes[8] & 0x3F) | 0x80);

		long mostSignificantBits = 0;
		long leastSignificantBits = 0;
		for (int i = 0; i < 8; i++) {
			mostSignificantBits = (mostSignificantBits << 8) | (bytes[i] & 0xFF);
		}
		for (int i = 8; i < 16; i++) {
			leastSignificantBits = (leastSignificantBits << 8) | (bytes[i] & 0xFF);
		}

		return new UUID(mostSignificantBits, leastSignificantBits).toString();
	}
}
