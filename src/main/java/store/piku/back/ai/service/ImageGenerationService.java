package store.piku.back.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import store.piku.back.ai.dto.AiDiaryResponseDTO;
import store.piku.back.ai.entity.DiaryImageGeneration;
import store.piku.back.diary.service.PhotoStorageService;
import store.piku.back.file.FileUtil;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.user.entity.User;
import org.springframework.stereotype.Service;
import store.piku.back.user.service.reader.UserReader;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImageGenerationService {

    private final GeminiApiClient geminiApiClient;
    private final DiaryImageGenerationService diaryImageGenerationService;
    private final FileUtil fileUtil;
    private final UserReader userReader;
    private final PhotoStorageService photoStorage;

    public AiDiaryResponseDTO diaryImage(String content, String userId, RequestMetaInfo requestMetaInfo) {
        log.info("사용자 ID '{}' 일기 이미지 생성 요청", userId);

        User user = userReader.getUserById(userId);
        String avatarPath = user.getAvatar();

        String characterImageBase64 = fileUtil.getImageAsBase64(avatarPath);
        if (characterImageBase64 == null || characterImageBase64.trim().isEmpty()) {
            log.error("사용자 '{}'의 아바타 이미지 파일을 읽을 수 없습니다. 경로: {}", userId, avatarPath);
            throw new IllegalArgumentException("사용자 아바타 이미지 파일을 읽을 수 없습니다. 경로: " + avatarPath);
        }
        log.info("사용자 '{}'의 아바타 이미지 로드 완료. Base64 길이: {}", userId, characterImageBase64.length());


        // 멀티모달 프롬프트 생성
        String prompt = buildActionPrompt(content, detectDesignTextIntent(content));
        log.info("일기 프롬프트 생성 완료: {}", prompt);

        String generatedImageRelativePath = generateCharacterActionImage(prompt, characterImageBase64, userId);
        if (generatedImageRelativePath == null) {
            throw new RuntimeException("Gemini API에서 이미지 생성을 실패했거나 파일 저장에 실패했습니다.");
        }

        // CalendarController
        String aiUrl = photoStorage.getPhotoUrl(generatedImageRelativePath);
        DiaryImageGeneration diaryImageGeneration = diaryImageGenerationService.save(userId, prompt, generatedImageRelativePath);
        log.info("생성된 이미지 URL: {}", aiUrl);
        return new AiDiaryResponseDTO(
                diaryImageGeneration.getId(),
                aiUrl,
                null
        );
    }

    /**
     * 캐릭터 행위 이미지 생성 (Gemini API 멀티모달)
     * 생성된 이미지의 저장 경로 (userId/filename.png)를 반환합니다.
     */
    private String generateCharacterActionImage(String prompt, String characterImageBase64, String userId) {
        log.info("Gemini API 호출 (멀티모달: 사용자 ID: {}", userId);

        try {
            // Gemini API 멀티모달 호출 (이미지 + 텍스트)
            String base64ImageData = geminiApiClient.editImage(characterImageBase64, prompt).block();

            if (base64ImageData != null && !base64ImageData.isEmpty()) {
                // FileUtil을 사용하여 Base64 데이터를 파일로 저장
                // fileUtil.saveBase64AsFile은 "userId/filename.ext" 형태의 경로 반환
                String savedFilePathSuffix = photoStorage.saveAIPhoto(base64ImageData, userId, "png");
                log.info("Gemini API 행위 이미지 생성 및 저장 성공: 사용자 ID: {}, 경로: {}", userId, savedFilePathSuffix);
                return savedFilePathSuffix; // "userId/filename.png" 반환
            } else {
                log.error("Gemini API에서 이미지 데이터를 받지 못했습니다. userId: {}", userId);
                throw new RuntimeException("Gemini API에서 이미지 데이터를 받지 못했습니다.");
            }

        } catch (Exception e) {
            log.error("Gemini API 일기 이미지 생성 중 오류 발생: 사용자 ID: {},", userId, e);
            throw new RuntimeException("일기 이미지 생성 실패: " + e.getMessage());
        }
    }

    /**
     * 액션 프롬프트 구성
     */
    public String buildActionPrompt(String content, boolean hasDesignText) {
        StringBuilder sb = new StringBuilder();

        // Task
        sb.append("Using the provided character reference image(s), ");
        sb.append("generate ONE image of the SAME character performing this diary action: ");
        sb.append(content).append(". ");

        // Core requirements
        sb.append("Follow ALL requirements: ");
        sb.append("• Identity & Style Consistency: Match the character’s facial features, hairstyle, skin tone, body proportions, clothing palette, and art style from the reference image(s). ");
        sb.append("• Scene: Build a coherent, artistic scene that supports the action with appropriate environment, props, and a natural pose. ");
        sb.append("• Composition & Camera: Use clear framing/angle that showcases the action; cinematic lighting that matches the mood; avoid awkward crops. ");
        sb.append("• Quality: High detail with clean rendering and correct anatomy/hands/fingers. ");

        // Conditional text rule
        sb.append("• Text Rendering Rule: ");
        if (hasDesignText) {
            sb.append("If the scene intentionally includes a design element that contains text (e.g., speech bubbles, signs, posters, UI panels, clothing prints), ");
            sb.append("you MAY render SHORT, readable text only INSIDE those elements. ");
        } else {
            sb.append("Render NO text anywhere: no words, letters, numbers, logos, watermarks, or captions. ");
            sb.append("If an object would normally have text, leave it blank or use non-legible abstract marks. ");
        }

        // Output & constraints
        sb.append("• Output: A single finished image only. ");
        sb.append("• Constraints: Respect the reference style/lighting/perspective; keep the background coherent but not distracting.");

        return sb.toString();
    }

    /**
     * 간단 키워드 기반 감지(초기 버전): '말풍선/간판/포스터/현수막/UI/프린트/텍스트' 등이 포함되면 true
     * 실제 서비스에서는 규칙 기반 + 사용자 체크박스/토글 병행 권장
     */
    public boolean detectDesignTextIntent(String content) {
        String lower = content.toLowerCase();
        return lower.contains("말풍선") || lower.contains("간판") || lower.contains("표지판")
                || lower.contains("사인") || lower.contains("포스터") || lower.contains("배너")
                || lower.contains("현수막") || lower.contains("간판문구")
                || lower.contains("ui") || lower.contains("패널")
                || lower.contains("티셔츠 프린트") || lower.contains("텍스트")
                || lower.contains("speech bubble") || lower.contains("sign")
                || lower.contains("poster") || lower.contains("banner") || lower.contains("caption");
    }
}

