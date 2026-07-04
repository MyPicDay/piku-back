package com.pikume.back.creative.application.port.out;

import com.pikume.back.creative.domain.DiaryImageGeneration;

/**
 * 생성 이력 기록 Outbound Port
 */
public interface RecordGenerationPort {

	DiaryImageGeneration recordGeneration(DiaryImageGeneration generation);
}
