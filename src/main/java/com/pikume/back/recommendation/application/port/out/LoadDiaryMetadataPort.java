package com.pikume.back.recommendation.application.port.out;

import com.pikume.back.recommendation.domain.DiaryMetadata;

import java.util.List;
import java.util.Optional;

/**
 * 일기 메타데이터 조회 Outbound Port
 */
public interface LoadDiaryMetadataPort {

	Optional<DiaryMetadata> loadByDiaryId(Long diaryId);

	List<DiaryMetadata> loadByDiaryIds(List<Long> diaryIds);
}
