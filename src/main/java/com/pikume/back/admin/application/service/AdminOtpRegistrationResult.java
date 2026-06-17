package com.pikume.back.admin.application.service;

public record AdminOtpRegistrationResult(
		String issuer,
		String accountName,
		String provisioningUri,
		String manualEntryKey
) {
}
