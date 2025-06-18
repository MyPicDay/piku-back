package store.piku.back.diary.service;


import org.springframework.web.multipart.MultipartFile;
import store.piku.back.diary.entity.Diary;

import java.io.IOException;
import java.util.List;


public interface PhotoStorage {
    void savePhoto(Diary diary, List<MultipartFile> photos, String userId, int coverPhotoIndex) throws IOException;
    void saveAiPhoto(Diary diary, List<Long> aiPhotos, String userId, int coverPhotoIndex) throws IOException;
}
