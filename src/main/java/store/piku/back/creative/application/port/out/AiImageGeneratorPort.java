package store.piku.back.creative.application.port.out;

import reactor.core.publisher.Mono;

/**
 * 외부 AI API 호출 Outbound Port
 */
public interface AiImageGeneratorPort {

	/**
	 * 텍스트 기반 캐릭터 분석
	 */
	Mono<String> analyzeTextForCharacter(String userText);

	/**
	 * 행위 설명 분석
	 */
	Mono<String> analyzeActionDescription(String actionDescription);

	/**
	 * 텍스트 기반 이미지 생성
	 */
	Mono<String> generateImage(String prompt);

	/**
	 * 멀티모달 이미지 편집 (이미지 + 텍스트)
	 * 
	 * @return Base64 이미지 데이터
	 */
	Mono<String> editImage(String imageBase64, String prompt);
}
