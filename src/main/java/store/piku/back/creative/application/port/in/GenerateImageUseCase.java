package store.piku.back.creative.application.port.in;

import store.piku.back.creative.domain.DiaryImageGeneration;
import store.piku.back.global.dto.RequestMetaInfo;

/**
 * 이미지 생성 Inbound Port
 */
public interface GenerateImageUseCase {

	/**
	 * 일기 내용 기반 AI 이미지 생성
	 * 
	 * @return 생성된 DiaryImageGeneration (id, url 포함)
	 */
	DiaryImageGeneration generateDiaryImage(String content, String userId, RequestMetaInfo requestMetaInfo);
}
