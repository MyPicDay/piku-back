package store.piku.back.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import store.piku.back.ai.dto.AiDiaryResponseDTO;
import store.piku.back.ai.entity.DiaryImageGeneration;
import store.piku.back.file.FileUtil;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.global.util.ImagePathToUrlConverter;
import store.piku.back.user.entity.User;
import org.springframework.stereotype.Service;
import store.piku.back.user.service.reader.UserReader;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImageGenerationService {

    private final ImagePathToUrlConverter imagePathToUrlConverter;
    private final GeminiApiClient geminiApiClient;
    private final DiaryImageGenerationService diaryImageGenerationService;
    private final FileUtil fileUtil;
    private final UserReader userReader;

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
        String prompt = buildActionPrompt(content);
        log.info("일기 프롬프트 생성 완료: {}", prompt);

        String generatedImageRelativePath = generateCharacterActionImage(prompt, characterImageBase64, userId);
        if (generatedImageRelativePath == null) {
            throw new RuntimeException("Gemini API에서 이미지 생성을 실패했거나 파일 저장에 실패했습니다.");
        }

        // CalendarController
        String imageUrl = imagePathToUrlConverter.diaryImageUrl(generatedImageRelativePath, requestMetaInfo);
        DiaryImageGeneration diaryImageGeneration = diaryImageGenerationService.save(userId, prompt, generatedImageRelativePath);
        log.info("생성된 이미지 URL: {}", imageUrl);
        AiDiaryResponseDTO responseDTO = new AiDiaryResponseDTO(
                diaryImageGeneration.getId(),
                imageUrl
        );
        return responseDTO;
    }

    /**
     * 캐릭터 행위 이미지 생성 (Gemini API 멀티모달)
     * 생성된 이미지의 저장 경로 (userId/filename.png)를 반환합니다.
     */
    private String generateCharacterActionImage(String prompt, String characterImageBase64, String userId) {
        log.info("Gemini API 호출 (멀티모달): 사용자 ID: {}", userId);

        try {
            // Gemini API 멀티모달 호출 (이미지 + 텍스트)
            String base64ImageData = geminiApiClient.editImage(characterImageBase64, prompt).block();

            if (base64ImageData != null && !base64ImageData.isEmpty()) {
                // FileUtil을 사용하여 Base64 데이터를 파일로 저장
                // fileUtil.saveBase64AsFile은 "userId/filename.ext" 형태의 경로 반환
                String savedFilePathSuffix = fileUtil.saveBase64AsFile(base64ImageData, userId, "png");
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
    private String buildActionPrompt(String content) {
        StringBuilder promptBuilder = new StringBuilder();

        promptBuilder.append("Based on the provided character image, ");
        promptBuilder.append("generate an image where the character is performing the following action: ");
        promptBuilder.append(content);


        promptBuilder.append(". Additional instructions: ");
        promptBuilder.append(". Please generate it as an artistic and visually appealing scene.");
        promptBuilder.append(". Maintain consistency with the character's appearance and style, and generate the image with high quality.");
        return promptBuilder.toString();
    }
}

