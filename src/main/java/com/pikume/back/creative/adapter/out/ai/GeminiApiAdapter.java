package com.pikume.back.creative.adapter.out.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import com.pikume.back.ai.service.GeminiApiClient;
import com.pikume.back.creative.application.port.out.AiImageGeneratorPort;

/**
 * Gemini API Adapter
 * 기존 GeminiApiClient를 래핑하여 AiImageGeneratorPort를 구현합니다.
 */
@Component
@RequiredArgsConstructor
public class GeminiApiAdapter implements AiImageGeneratorPort {

	private final GeminiApiClient geminiApiClient;

	@Override
	public Mono<String> analyzeTextForCharacter(String userText) {
		return geminiApiClient.analyzeTextForCharacter(userText);
	}

	@Override
	public Mono<String> analyzeActionDescription(String actionDescription) {
		return geminiApiClient.analyzeActionDescription(actionDescription);
	}

	@Override
	public Mono<String> generateImage(String prompt) {
		return geminiApiClient.generateImage(prompt);
	}

	@Override
	public Mono<String> editImage(String imageBase64, String prompt) {
		return geminiApiClient.editImage(imageBase64, prompt);
	}
}
