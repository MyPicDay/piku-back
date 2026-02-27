package com.pikume.back.recommendation.application.port.out;

import com.pikume.back.recommendation.domain.DiaryMetadata;

/**
 * 콘텐츠 분석 Outbound Port
 */
public interface ContentAnalyzerPort {

	DiaryMetadata analyze(Long diaryId, String content);
}
