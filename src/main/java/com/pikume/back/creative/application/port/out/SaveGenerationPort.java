package com.pikume.back.creative.application.port.out;

import com.pikume.back.creative.domain.DiaryImageGeneration;

/**
 * 생성 이력 저장 Outbound Port
 */
public interface SaveGenerationPort {

	DiaryImageGeneration save(DiaryImageGeneration generation);
}
