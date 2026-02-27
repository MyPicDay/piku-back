package com.pikume.back.recommendation.application.port.out;

import com.pikume.back.recommendation.domain.DiaryMetadata;

/**
 * 일기 메타데이터 저장 Outbound Port
 */
public interface SaveDiaryMetadataPort {

	DiaryMetadata save(DiaryMetadata metadata);
}
