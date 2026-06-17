package com.pikume.back.admin.application.port.out;

public interface AdminOtpPort {

	String generateSecret();

	String provisioningUri(String issuer, String accountName, String secret);

	boolean verify(String secret, String code);
}
