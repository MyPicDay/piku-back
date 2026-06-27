package com.pikume.back.admin.application.service;

import java.time.LocalDate;

public record AdminSignupBucketResult(
		String bucket,
		LocalDate startDate,
		LocalDate endDate,
		long signupMembers
) {
}
