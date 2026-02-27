package com.pikume.back.diary.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiaryMonthCountDTO {
	private int year;
	private int month;
	private Long count;

	public DiaryMonthCountDTO(Integer year, Integer month, Long count) {
		this.year = year != null ? year : 0;
		this.month = month != null ? month : 0;
		this.count = count != null ? count : 0L;
	}
}
