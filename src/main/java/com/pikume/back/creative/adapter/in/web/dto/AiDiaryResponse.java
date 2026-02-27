package com.pikume.back.creative.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * AI 일기 이미지 생성 응답 DTO
 */
@AllArgsConstructor
@Data
public class AiDiaryResponse {
	private Long id;
	private String url;
	private String message;
}
