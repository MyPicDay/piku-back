package store.piku.back.recommendation.application.port.out;

import store.piku.back.recommendation.domain.DiaryMetadata;

/**
 * 콘텐츠 분석 Outbound Port
 */
public interface ContentAnalyzerPort {

	DiaryMetadata analyze(Long diaryId, String content);
}
