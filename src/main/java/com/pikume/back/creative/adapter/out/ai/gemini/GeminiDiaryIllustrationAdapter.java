package com.pikume.back.creative.adapter.out.ai.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import com.pikume.back.creative.application.dto.DiaryIllustrationRequest;
import com.pikume.back.creative.application.dto.GeneratedIllustrationPayload;
import com.pikume.back.creative.application.port.out.GenerateDiaryIllustrationPort;
import com.pikume.back.creative.domain.exception.ImageGenerationException;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class GeminiDiaryIllustrationAdapter implements GenerateDiaryIllustrationPort {

	private final WebClient.Builder webClientBuilder;
	private final ObjectMapper objectMapper;

	@Value("${gemini.api.key}")
	private String apiKey;

	@Value("${gemini.api.base-url}")
	private String baseUrl;

	@Value("${gemini.api.models.image-generation}")
	private String imageGenerationModel;

	@Value("${gemini.api.timeout:30s}")
	private Duration timeout;

	@Override
	public GeneratedIllustrationPayload generate(DiaryIllustrationRequest request) {
		log.info("Gemini 일기 이미지 생성 요청");

		try {
			String response = webClientBuilder
					.baseUrl(baseUrl)
					.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
					.build()
					.post()
					.uri(String.format("/models/%s:generateContent?key=%s", imageGenerationModel, apiKey))
					.bodyValue(createImageEditRequest(request.referenceImageBase64(), request.prompt()))
					.retrieve()
					.bodyToMono(String.class)
					.timeout(timeout)
					.doOnError(WebClientResponseException.class, error -> log.error(
							"Gemini API 이미지 생성 HTTP 오류: status={}, body={}",
							error.getStatusCode(),
							error.getResponseBodyAsString()))
					.block();

			if (response == null || response.isBlank()) {
				throw new ImageGenerationException("AI 이미지 생성 응답이 비어 있습니다.");
			}

			String imageBase64 = extractImageFromResponse(response);
			if (imageBase64.isBlank()) {
				throw new ImageGenerationException("AI 이미지 데이터가 비어 있습니다.");
			}

			return new GeneratedIllustrationPayload(imageBase64, "png");
		} catch (ImageGenerationException e) {
			throw e;
		} catch (Exception e) {
			log.error("Gemini 일기 이미지 생성 실패", e);
			throw new ImageGenerationException("AI 이미지 생성에 실패했습니다.", e);
		}
	}

	private Map<String, Object> createImageEditRequest(String imageBase64, String prompt) {
		Map<String, Object> request = new HashMap<>();

		Map<String, Object> textPart = new HashMap<>();
		textPart.put("text", prompt);

		Map<String, Object> imagePart = new HashMap<>();
		Map<String, Object> inlineData = new HashMap<>();
		inlineData.put("mime_type", "image/png");
		inlineData.put("data", imageBase64);
		imagePart.put("inlineData", inlineData);

		Map<String, Object> content = new HashMap<>();
		content.put("parts", List.of(textPart, imagePart));
		request.put("contents", List.of(content));

		Map<String, Object> generationConfig = new HashMap<>();
		generationConfig.put("responseModalities", List.of("TEXT", "IMAGE"));
		request.put("generationConfig", generationConfig);

		return request;
	}

	private String extractImageFromResponse(String response) {
		try {
			JsonNode jsonNode = objectMapper.readTree(response);
			JsonNode candidates = jsonNode.get("candidates");

			if (candidates != null && candidates.isArray() && !candidates.isEmpty()) {
				JsonNode content = candidates.get(0).get("content");
				if (content != null) {
					JsonNode parts = content.get("parts");
					if (parts != null && parts.isArray()) {
						for (JsonNode part : parts) {
							JsonNode inlineData = part.get("inlineData");
							if (inlineData == null) {
								inlineData = part.get("inline_data");
							}

							if (inlineData != null && inlineData.get("data") != null) {
								return inlineData.get("data").asText();
							}
						}
					}
				}
			}

			throw new ImageGenerationException("AI 이미지 데이터가 응답에 포함되지 않았습니다.");
		} catch (ImageGenerationException e) {
			throw e;
		} catch (Exception e) {
			log.error("Gemini 이미지 응답 파싱 실패", e);
			throw new ImageGenerationException("AI 이미지 응답 파싱에 실패했습니다.", e);
		}
	}
}
