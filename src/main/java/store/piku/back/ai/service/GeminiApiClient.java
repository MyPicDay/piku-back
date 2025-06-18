package store.piku.back.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gemini API 직접 호출 클라이언트
 * 
 * 참고: https://ai.google.dev/gemini-api/docs/image-generation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiApiClient {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.base-url}")
    private String baseUrl;

    @Value("${gemini.api.models.image-generation}")
    private String imageGenerationModel;

    @Value("${gemini.api.models.chat}")
    private String chatModel;

    @Value("${gemini.api.timeout:30s}")
    private Duration timeout;

    /**
     * 텍스트 분석 - 일기나 묘사에서 캐릭터 정보 추출
     * 
     * @param userText 사용자가 작성한 일기나 캐릭터 묘사
     * @return 이미지 생성에 적합한 캐릭터 프롬프트
     */
    public Mono<String> analyzeTextForCharacter(String userText) {
        log.info("Gemini API 텍스트 분석 요청: model={}, 텍스트 길이={}", chatModel, userText.length());

        WebClient webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        // 텍스트 분석 요청 본문 구성
        Map<String, Object> requestBody = createTextAnalysisRequest(userText);

        String endpoint = String.format("/models/%s:generateContent?key=%s", chatModel, apiKey);

        return webClient.post()
                .uri(endpoint)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(timeout)
                .doOnNext(response -> {
                    log.info("Gemini API 텍스트 분석 응답 수신: 응답 크기={} bytes", response.length());
                    log.debug("텍스트 분석 응답 내용: {}", response);
                })
                .map(this::extractTextFromResponse)
                .doOnError(WebClientResponseException.class, error -> {
                    log.error("Gemini API 텍스트 분석 HTTP 오류: status={}, body={}", error.getStatusCode(), error.getResponseBodyAsString());
                })
                .doOnError(error -> log.error("Gemini API 텍스트 분석 실패: ", error));
    }

    /**
     * 행위 설명 분석 - 캐릭터 행동을 이미지 생성에 적합한 묘사로 변환
     * 
     * @param actionDescription 사용자가 입력한 행위 설명
     * @return 이미지 편집에 적합한 행위 프롬프트
     */
    public Mono<String> analyzeActionDescription(String actionDescription) {
        log.info("Gemini API 행위 분석 요청: model={}, 행위 설명={}", chatModel, actionDescription);

        WebClient webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        // 행위 분석 요청 본문 구성
        Map<String, Object> requestBody = createActionAnalysisRequest(actionDescription);

        String endpoint = String.format("/models/%s:generateContent?key=%s", chatModel, apiKey);

        return webClient.post()
                .uri(endpoint)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(timeout)
                .doOnNext(response -> {
                    log.info("Gemini API 행위 분석 응답 수신: 응답 크기={} bytes", response.length());
                    log.debug("행위 분석 응답 내용: {}", response);
                })
                .map(this::extractTextFromResponse)
                .doOnError(WebClientResponseException.class, error -> {
                    log.error("Gemini API 행위 분석 HTTP 오류: status={}, body={}", error.getStatusCode(), error.getResponseBodyAsString());
                })
                .doOnError(error -> log.error("Gemini API 행위 분석 실패: ", error));
    }

    /**
     * 텍스트 분석 요청 본문 생성
     */
    private Map<String, Object> createTextAnalysisRequest(String userText) {
        Map<String, Object> request = new HashMap<>();
        
        // 구문 분석을 위한 시스템 프롬프트 생성
        String analysisPrompt = createCharacterAnalysisPrompt(userText);
        
        // contents 배열
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", analysisPrompt);
        content.put("parts", List.of(part));
        request.put("contents", List.of(content));

        // 텍스트 응답만 필요
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.7);
        generationConfig.put("maxOutputTokens", 1000);
        request.put("generationConfig", generationConfig);

        log.debug("Gemini API 텍스트 분석 요청 본문: {}", request);
        return request;
    }

    /**
     * 캐릭터 분석을 위한 프롬프트 생성
     */
    private String createCharacterAnalysisPrompt(String userText) {
        return String.format("""
            다음 텍스트를 분석하여 이미지 생성에 적합한 캐릭터 묘사를 추출해주세요.
            
            === 사용자 텍스트 ===
            %s
            
            === 지시사항 ===
            1. 텍스트에 **명시적으로 언급된 내용만** 추출하세요
            2. 성별, 나이, 외모, 의상 등은 텍스트에 없으면 추가하지 마세요
            3. 단순히 추론이나 가정으로 내용을 확장하지 마세요
            4. 상황이나 장소가 언급된 경우에만 배경을 간략히 포함하세요
            5. 애니메이션 스타일의 캐릭터로 생성하되, 과도한 디테일은 피하세요
            6. 감정이나 행동이 명확하게 드러난 경우에만 포함하세요
            
            === 출력 형식 ===
            간결하고 핵심적인 영어 프롬프트만 출력하세요. 추측이나 추가 묘사는 포함하지 마세요.
            
            예시 입력: "친구와 카페에서 만났어"
            예시 출력: "anime character at cafe, meeting with friend"
            
            예시 입력: "긴 머리 소녀가 웃고 있었어"  
            예시 출력: "anime girl with long hair, smiling"
            """, userText);
    }

    /**
     * Gemini API 응답에서 텍스트 추출
     */
    private String extractTextFromResponse(String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            
            log.debug("텍스트 분석 응답 구조 분석 시작");
            
            JsonNode candidates = jsonNode.get("candidates");
            if (candidates != null && candidates.isArray() && !candidates.isEmpty()) {
                JsonNode content = candidates.get(0).get("content");
                if (content != null) {
                    JsonNode parts = content.get("parts");
                    if (parts != null && parts.isArray()) {
                        for (JsonNode part : parts) {
                            JsonNode textNode = part.get("text");
                            if (textNode != null) {
                                String extractedText = textNode.asText().trim();
                                log.info("텍스트 분석 완료: 추출된 프롬프트={}", extractedText);
                                return extractedText;
                            }
                        }
                    }
                }
            }
            
            log.error("텍스트 분석 응답에서 텍스트를 찾을 수 없습니다: {}", response);
            throw new RuntimeException("텍스트 분석 응답이 올바르지 않습니다.");
            
        } catch (Exception e) {
            log.error("텍스트 분석 응답 파싱 오류: ", e);
            throw new RuntimeException("텍스트 분석 실패: " + e.getMessage());
        }
    }

    /**
     * Gemini 2.0 Flash를 사용한 이미지 생성
     * 
     * @param prompt 이미지 생성 프롬프트
     * @return 생성된 이미지 Base64 데이터
     */
    public Mono<String> generateImage(String prompt) {
        log.info("Gemini API 이미지 생성 요청: model={}, prompt={}", imageGenerationModel, prompt);

        WebClient webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        // Gemini API 요청 본문 구성
        Map<String, Object> requestBody = createImageGenerationRequest(prompt);

        String endpoint = String.format("/models/%s:generateContent?key=%s", imageGenerationModel, apiKey);

        return webClient.post()
                .uri(endpoint)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(timeout)
                .doOnNext(response -> {
                    log.info("Gemini API 응답 수신 완료: 응답 크기={} bytes", response.length());
                    log.debug("Gemini API 응답 내용: {}", response.length() > 1000 ? response.substring(0, 1000) + "..." : response);
                })
                .map(this::extractImageFromResponse)
                .doOnError(WebClientResponseException.class, error -> log.error("Gemini API HTTP 오류 발생: status={}, body={}", error.getStatusCode(), error.getResponseBodyAsString()))
                .doOnError(error -> log.error("Gemini API 호출 실패: ", error));
    }

    /**
     * Gemini API 이미지 생성 요청 본문 생성
     */
    private Map<String, Object> createImageGenerationRequest(String prompt) {
        Map<String, Object> request = new HashMap<>();
        
        // contents 배열
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);
        content.put("parts", List.of(part));
        request.put("contents", List.of(content));

        // generationConfig - responseModalities 설정 (문서에 따라 필수)
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("responseModalities", List.of("TEXT", "IMAGE"));
        request.put("generationConfig", generationConfig);

        log.debug("Gemini API 요청 본문: {}", request);
        return request;
    }

    /**
     * Gemini API 응답에서 이미지 데이터 추출
     */
    private String extractImageFromResponse(String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            
            // 응답 구조 로깅
            log.debug("Gemini API 응답 구조 분석 시작");
            
            // candidates[0].content.parts에서 이미지 데이터 찾기
            JsonNode candidates = jsonNode.get("candidates");
            if (candidates != null && candidates.isArray() && !candidates.isEmpty()) {
                log.debug("candidates 배열 발견: 개수={}", candidates.size());
                
                JsonNode content = candidates.get(0).get("content");
                if (content != null) {
                    JsonNode parts = content.get("parts");
                    if (parts != null && parts.isArray()) {
                        log.debug("parts 배열 발견: 개수={}", parts.size());
                        
                        for (int i = 0; i < parts.size(); i++) {
                            JsonNode part = parts.get(i);
                            log.debug("part[{}] 필드들: {}", i, part.fieldNames().toString());
                            
                            // inlineData 또는 inline_data 필드 확인
                            JsonNode inlineData = part.get("inlineData");
                            if (inlineData == null) {
                                inlineData = part.get("inline_data");
                            }
                            
                            if (inlineData != null) {
                                JsonNode data = inlineData.get("data");
                                if (data != null) {
                                    String base64Data = data.asText();
                                    log.info("Gemini API에서 이미지 생성 성공: Base64 데이터 길이={} bytes", base64Data.length());
                                    return base64Data;
                                }
                            }
                            
                            // 텍스트 파트 로깅
                            JsonNode textNode = part.get("text");
                            if (textNode != null) {
                                log.debug("텍스트 응답 발견: {}", textNode.asText());
                            }
                        }
                    } else {
                        log.warn("content.parts가 null이거나 배열이 아닙니다");
                    }
                } else {
                    log.warn("candidates[0].content가 null입니다");
                }
            } else {
                log.warn("candidates가 null이거나 빈 배열입니다");
            }
            
            log.error("Gemini API 응답에서 이미지 데이터를 찾을 수 없습니다. 전체 응답: {}", response);
            throw new RuntimeException("이미지 데이터가 응답에 포함되지 않았습니다.");
            
        } catch (Exception e) {
            log.error("Gemini API 응답 파싱 오류: ", e);
            throw new RuntimeException("응답 파싱 실패: " + e.getMessage());
        }
    }

    /**
     * 멀티모달 이미지 편집 (이미지 + 텍스트)
     */
    public Mono<String> editImage(String imageBase64, String prompt) {
        log.info("Gemini API 이미지 편집 요청: prompt={}", prompt);

        WebClient webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> requestBody = createImageEditRequest(imageBase64, prompt);
        String endpoint = String.format("/models/%s:generateContent?key=%s", imageGenerationModel, apiKey);

        return webClient.post()
                .uri(endpoint)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(timeout)
                .doOnNext(response -> {
                    log.info("Gemini API 이미지 편집 응답 수신: 응답 크기={} bytes", response.length());
                    log.debug("Gemini API 이미지 편집 응답: {}", response.length() > 1000 ? response.substring(0, 1000) + "..." : response);
                })
                .map(this::extractImageFromResponse)
                .doOnError(WebClientResponseException.class, error -> log.error("Gemini API 이미지 편집 HTTP 오류: status={}, body={}", error.getStatusCode(), error.getResponseBodyAsString()))
                .doOnError(error -> log.error("Gemini API 이미지 편집 실패: ", error));
    }

    /**
     * 이미지 편집 요청 본문 생성
     */
    private Map<String, Object> createImageEditRequest(String imageBase64, String prompt) {
        Map<String, Object> request = new HashMap<>();
        
        // 텍스트 파트
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);
        
        // 이미지 파트
        Map<String, Object> imagePart = new HashMap<>();
        Map<String, Object> inlineData = new HashMap<>();
        inlineData.put("mime_type", "image/png");
        inlineData.put("data", imageBase64);
        imagePart.put("inlineData", inlineData);
        
        // contents 배열
        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(textPart, imagePart));
        request.put("contents", List.of(content));

        // generationConfig
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("responseModalities", List.of("TEXT", "IMAGE"));
        request.put("generationConfig", generationConfig);

        log.debug("Gemini API 이미지 편집 요청 본문 생성 완료");
        return request;
    }

    /**
     * 행위 분석을 위한 프롬프트 생성
     */
    private String createActionAnalysisPrompt(String actionDescription) {
        return String.format("""
            다음 행위/동작 설명을 이미지 편집에 적합한 상세한 묘사로 개선해주세요.
            
            === 행위 설명 ===
            %s
            
            === 지시사항 ===
            1. 행위/동작/자세에만 집중하여 분석하세요
            2. 캐릭터의 외모나 특징은 변경하지 마세요
            3. 동작의 역동성, 표정, 자세를 구체적으로 묘사하세요
            4. 이미지 편집에 적합한 영어 프롬프트로 변환하세요
            5. 부적절한 내용은 제외하고 긍정적인 동작으로 변환하세요
            
            === 출력 형식 ===
            간결하고 명확한 영어 프롬프트만 출력하세요. 설명이나 추가 텍스트는 포함하지 마세요.
            
            예시: "jumping with arms raised high, happy expression, dynamic pose, motion blur effect"
            """, actionDescription);
    }

    /**
     * 행위 분석 요청 본문 생성
     */
    private Map<String, Object> createActionAnalysisRequest(String actionDescription) {
        Map<String, Object> request = new HashMap<>();
        
        // 행위 분석을 위한 시스템 프롬프트 생성
        String analysisPrompt = createActionAnalysisPrompt(actionDescription);
        
        // contents 배열
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", analysisPrompt);
        content.put("parts", List.of(part));
        request.put("contents", List.of(content));

        // 텍스트 응답만 필요
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.7);
        generationConfig.put("maxOutputTokens", 500);
        request.put("generationConfig", generationConfig);

        log.debug("Gemini API 행위 분석 요청 본문: {}", request);
        return request;
    }
} 