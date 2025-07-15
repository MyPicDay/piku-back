package store.piku.back.diary.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import store.piku.back.ai.entity.DiaryImageGeneration;
import store.piku.back.ai.service.DiaryImageGenerationService;
import store.piku.back.diary.entity.Diary;
import store.piku.back.diary.entity.Photo;
import store.piku.back.diary.repository.PhotoRepository;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalPhotoStorage implements PhotoStorage{

    private final PhotoUtil photoUtil;
    private final PhotoRepository photoRepository;
    private final DiaryImageGenerationService diaryImageGenerationService;

    @Override
    public void savePhoto(Diary diary, List<MultipartFile> photos, String userId, int coverPhotoIndex) throws IOException {
        log.info("사진 저장 시작 - 사용자: {}, 일기 날짜: {}, 사진 개수: {}", userId, diary.getDate(), photos.size());

        int i = 0;
        for (MultipartFile photo : photos) {
            if (!photo.isEmpty()) {
                String originalFilename = photo.getOriginalFilename();
                String filename = photoUtil.generateFileName(diary.getDate(), originalFilename);

                String filePath = photoUtil.saveToLocal(photo, userId, filename);
                Photo entity = photoRepository.save(new Photo(diary, filePath));
                if (coverPhotoIndex == i){
                    entity.updateRepresent(true);
                }
                i++;
            } else {
                log.warn("빈 파일 발견 - 사용자: {}, 일기 날짜: {}", userId, diary.getDate());
            }
        }
    }

    @Override
    public void saveAiPhoto(Diary diary, List<Long> aiPhotos, String userId, int coverPhotoIndex) throws IOException {
        log.info("AI 사진 저장 시작 - 사용자: {}, 일기 날짜: {}, AI 사진 개수: {}", userId, diary.getDate(), aiPhotos.size());

        int i = 0;
        for (Long aiPhotoId : aiPhotos) {
            if (aiPhotoId != null) {
                DiaryImageGeneration diaryImageGeneration = diaryImageGenerationService.findById(aiPhotoId);
                String filePath = diaryImageGeneration.getFilePath();

                Photo entity = photoRepository.save(new Photo(diary, filePath));
                if (coverPhotoIndex == i){
                    entity.updateRepresent(true);
                }
                diaryImageGenerationService.updateDiaryId(aiPhotoId, diary.getId());
                i++;
            } else {
                log.warn("빈 AI 사진 ID 발견 - 사용자: {}, 일기 날짜: {}", userId, diary.getDate());
            }
        }
    }

}