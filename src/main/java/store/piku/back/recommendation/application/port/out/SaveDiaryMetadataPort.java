package store.piku.back.recommendation.application.port.out;

import store.piku.back.recommendation.domain.DiaryMetadata;

/**
 * 일기 메타데이터 저장 Outbound Port
 */
public interface SaveDiaryMetadataPort {

	DiaryMetadata save(DiaryMetadata metadata);
}
