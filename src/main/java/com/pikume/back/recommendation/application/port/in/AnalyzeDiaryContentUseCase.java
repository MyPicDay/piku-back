package com.pikume.back.recommendation.application.port.in;

import com.pikume.back.recommendation.application.dto.DiaryMetadataResult;

import java.util.Optional;

/**
 * 일기 콘텐츠 분석 Inbound Port
 */
public interface AnalyzeDiaryContentUseCase {

	void analyzeAndSave(Long diaryId, String content);

	Optional<DiaryMetadataResult> getMetadata(Long diaryId);
}
