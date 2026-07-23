package com.pikume.back.recommendation.application.port.in;

/**
 * 일기 콘텐츠 분석 Inbound Port
 */
public interface AnalyzeDiaryContentUseCase {

	void analyzeDiaryContent(Long diaryId, String content);
}
