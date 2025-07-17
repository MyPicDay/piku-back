package store.piku.back.diary.service;


import org.springframework.web.multipart.MultipartFile;
import store.piku.back.diary.entity.Diary;

import java.io.IOException;
import java.util.List;


public interface PhotoStorage {
    void oldSavePhoto(Diary diary, List<MultipartFile> photos, String userId, int coverPhotoIndex) throws IOException;
    void oldSaveAiPhoto(Diary diary, List<Long> aiPhotos, String userId, int coverPhotoIndex) throws IOException;
    void savePhoto(Diary diary, MultipartFile photo, String userId, Integer order) throws IOException;
    void saveAiPhoto(Diary diary, Long aiPhoto, String userId, Integer order);
}
