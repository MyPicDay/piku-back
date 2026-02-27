package com.pikume.back.recommendation.application.port.out;

import com.pikume.back.recommendation.domain.DiaryMetadata;

import java.util.List;
import java.util.Optional;

/**
 * 일기 메타데이터 조회 Outbound Port
 */
public interface LoadDiaryMetadataPort {

	Optional<DiaryMetadata> findByDiaryId(Long diaryId);

	List<DiaryMetadata> findByDiaryIds(List<Long> diaryIds);

	List<DiaryMetadata> findByPrimaryTopic(String topic);

	boolean existsByDiaryId(Long diaryId);
}
