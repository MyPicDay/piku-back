package store.piku.back.diary.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import store.piku.back.diary.entity.Diary;
import store.piku.back.diary.entity.Photo;
import store.piku.back.diary.repository.PhotoRepository;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class LocalPhotoStorage implements PhotoStorage{

    private final PhotoUtil photoUtil;
    private final PhotoRepository photoRepository;

    public LocalPhotoStorage(PhotoUtil photoUtil, PhotoRepository photoRepository) {
        this.photoUtil = photoUtil;
        this.photoRepository = photoRepository;
    }

    @Override
    public void savePhoto(Diary diary, List<MultipartFile> photos, String userId) throws IOException {
        log.info("사진 저장 시작 - 사용자: {}, 일기 날짜: {}, 사진 개수: {}", userId, diary.getDate(), photos.size());

        for (MultipartFile photo : photos) {
            if (!photo.isEmpty()) {
                String originalFilename = photo.getOriginalFilename();
                String filename = photoUtil.generateFileName(diary.getDate(), originalFilename);

                String filePath = photoUtil.saveToLocal(photo, userId, filename);

                photoRepository.save(new Photo(diary, filePath));
            } else {
                log.warn("빈 파일 발견 - 사용자: {}, 일기 날짜: {}", userId, diary.getDate());
            }
        }
    }
    }


