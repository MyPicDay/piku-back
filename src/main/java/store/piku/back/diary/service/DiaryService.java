package store.piku.back.diary.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import store.piku.back.diary.dto.DiaryDTO;
import store.piku.back.diary.dto.ResponseDTO;
import store.piku.back.diary.entity.Diary;
import store.piku.back.diary.entity.Photo;
import store.piku.back.diary.repository.DiaryRepository;
import store.piku.back.diary.repository.PhotoRepository;
import store.piku.back.user.entity.User;
import store.piku.back.user.repository.UserRepository;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final PhotoRepository photoRepository;
    private final UserRepository userRepository;
    private final PhotoStorage photoStorage;

    @Transactional
    public boolean createDiary(DiaryDTO diaryDTO, List<MultipartFile> photos, String userId) {

        log.info("사용자 [{}] - 일기 등록 시작. 내용: {}, 사진 개수: {}, 등록 날짜: {}",
                userId, diaryDTO.getContent(), diaryDTO.getPhotos().size(), diaryDTO.getDate());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. "));
        log.info(userId +"님 사용자 조회 완료");

        Diary diary = new Diary(diaryDTO.getContent(), diaryDTO.getStatus(),diaryDTO.getDate(),user );
        diary = diaryRepository.save(diary);
        log.info("사용자 [{}] - 일기 저장 완료. 일기 ID: {}", userId, diary.getId());

        try {
            photoStorage.savePhoto(diary, photos, userId);
            log.info("사용자 [{}] - 사진 저장 완료. 사진 개수: {}", userId, photos.size());
        } catch (IOException e) {
            log.error("사진 저장 실패 : {}", e.getMessage(), e);
            throw new RuntimeException("사진 저장 실패", e); // 트랜잭션 롤백
        }
        return true;
    }


    @Transactional(readOnly = true)
    public ResponseDTO getDiaryWithPhotos(Integer diaryId) {

        log.info( diaryId + " 일기 내용 조회 요청");
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new EntityNotFoundException("Diary not found"));
        log.info(diaryId+"일기 조회 완료");

        List<Photo> photos = photoRepository.findByDiaryId(diaryId);
        if (photos == null || photos.isEmpty()) {
            log.warn("DiaryId {} 에 해당하는 사진이 없음!", diaryId);
            throw new EntityNotFoundException("Photos not found for diaryId: " + diaryId);
        }
        log.info( diaryId+"일기 결합 사진 조회 완료 ");

        List<String> photoUrls = photos.stream().map(Photo::getUrl).collect(Collectors.toList());
        log.info("조회된 사진 개수: {}, URLs: {}", photos.size(), photoUrls);

        return new ResponseDTO(
                diary.getStatus(),
                diary.getContent(),
                photoUrls,
                diary.getDate()
        );
    }
}
