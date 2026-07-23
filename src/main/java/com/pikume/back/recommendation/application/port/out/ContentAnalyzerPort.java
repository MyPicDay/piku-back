package com.pikume.back.recommendation.application.port.out;

import com.pikume.back.recommendation.application.dto.DiaryContentAnalysis;

/**
 * 콘텐츠 분석 Outbound Port
 */
public interface ContentAnalyzerPort {

	DiaryContentAnalysis analyze(String content);
}
