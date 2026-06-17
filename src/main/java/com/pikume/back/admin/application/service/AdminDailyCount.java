package com.pikume.back.admin.application.service;

import java.time.LocalDate;

public record AdminDailyCount(
		LocalDate date,
		long count
) {
}
