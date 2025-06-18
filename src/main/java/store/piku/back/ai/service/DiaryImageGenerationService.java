package store.piku.back.ai.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import store.piku.back.ai.entity.DiaryImageGeneration;
import store.piku.back.ai.repository.DiaryImageGenerationRepository;
import store.piku.back.global.config.CustomUserDetails;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.global.util.ImagePathToUrlConverter;
import store.piku.back.global.util.RequestMetaMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiaryImageGenerationService {

    private final DiaryImageGenerationRepository diaryImageGenerationRepository;
    private final RequestMetaMapper requestMetaMapper;
    private final ImagePathToUrlConverter imagePathToUrlConverter;

    public DiaryImageGeneration save(String userId, String prompt, String filePath) {
        DiaryImageGeneration diaryImageGeneration = new DiaryImageGeneration(
                userId,
                prompt,
                filePath
        );
        return diaryImageGenerationRepository.save(diaryImageGeneration);
    }

    public DiaryImageGeneration findById(Long id) {
        return diaryImageGenerationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("DiaryImageGeneration not found with id: " + id));
    }

    public void updateDiaryId(Long historyId, Long diaryId) {
        DiaryImageGeneration diaryImageGeneration = findById(historyId);
        diaryImageGeneration.saveDiaryId(diaryId);
        diaryImageGenerationRepository.save(diaryImageGeneration);
    }


    // 일기에 저장되지 않은 이미지 파일들
    public List<DiaryImageGeneration> notSaveImageFile() {
        List<DiaryImageGeneration> diaryImageGeneration = diaryImageGenerationRepository.findByDiaryIdIsNull();
        return diaryImageGeneration;
    }

    // userId와 생성된 imageUrl로 DiaryImageGeneration을 찾는 메소드
    public DiaryImageGeneration getByUserIdAndFilePath(String userId, String filePath) {
        return diaryImageGenerationRepository.findByUserIdAndFilePath(userId, filePath)
                .orElseThrow(() -> new RuntimeException("DiaryImageGeneration not found for userId: " + userId + " and filePath: " + filePath));
    }

    public String getPathFromUrl(String aiGeneratedImageUrl, HttpServletRequest request) {
        RequestMetaInfo requestMetaInfo = requestMetaMapper.extractMetaInfo(request);
        if (aiGeneratedImageUrl == null || aiGeneratedImageUrl.isEmpty()) {
            throw new RuntimeException("AI generated image URL is null or empty");
        }
        String path = imagePathToUrlConverter.extractImagePathFromUrl(aiGeneratedImageUrl, requestMetaInfo);
        if (path == null || path.isEmpty()) {
            throw new RuntimeException("Failed to extract image path from URL: " + aiGeneratedImageUrl);
        }
        return path;
    }

    public void diaryUpdate(CustomUserDetails customUserDetails, Long diaryId, String path) {
        String userId = customUserDetails.getId();
        DiaryImageGeneration diaryImageGeneration = getByUserIdAndFilePath(userId, path);
        updateDiaryId(diaryImageGeneration.getId(), diaryId);
    }
}
