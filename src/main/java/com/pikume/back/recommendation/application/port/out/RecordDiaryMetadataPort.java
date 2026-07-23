package com.pikume.back.recommendation.application.port.out;

import com.pikume.back.recommendation.domain.DiaryMetadata;

public interface RecordDiaryMetadataPort {

	DiaryMetadata recordDiaryMetadata(DiaryMetadata metadata);
}
