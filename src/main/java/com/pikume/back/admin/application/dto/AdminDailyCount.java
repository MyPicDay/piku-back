package com.pikume.back.admin.application.dto;

import java.time.LocalDate;

public record AdminDailyCount(
		LocalDate date,
		long count
) {
}
