package store.piku.back.creative.application.port.out;

import store.piku.back.creative.domain.DiaryImageGeneration;

/**
 * 생성 이력 저장 Outbound Port
 */
public interface SaveGenerationPort {

	DiaryImageGeneration save(DiaryImageGeneration generation);
}
