package com.pikume.back.recommendation.application.port.in;

import com.pikume.back.recommendation.application.dto.DiaryMetadataResult;

import java.util.Optional;

public interface QueryDiaryMetadataUseCase {

	Optional<DiaryMetadataResult> queryDiaryMetadata(Long diaryId);
}
