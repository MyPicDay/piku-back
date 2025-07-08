package store.piku.back.diary.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import store.piku.back.diary.dto.CalendarDiaryResponseDTO;
import store.piku.back.diary.dto.DiaryDTO;
import store.piku.back.diary.dto.ResponseDTO;
import store.piku.back.diary.dto.ResponseDiaryDTO;
import store.piku.back.diary.entity.Diary;
import store.piku.back.diary.entity.Photo;
import store.piku.back.diary.enums.DiaryPhotoType;
import store.piku.back.diary.enums.FriendStatus;
import store.piku.back.diary.enums.Status;
import store.piku.back.diary.exception.DiaryNotFoundException;
import store.piku.back.diary.exception.DuplicateDiaryException;
import store.piku.back.diary.repository.DiaryRepository;
import store.piku.back.diary.repository.PhotoRepository;
import store.piku.back.file.FileUtil;
import store.piku.back.friend.service.FriendRequestService;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.global.util.ImagePathToUrlConverter;
import store.piku.back.user.entity.User;
import store.piku.back.user.exception.UserNotFoundException;
import store.piku.back.user.service.UserService;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@Slf4j
@RequiredArgsConstructor
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final PhotoRepository photoRepository;
    private final UserService userService;
    private final PhotoStorage photoStorage;
    private final ImagePathToUrlConverter imagePathToUrlConverter;
    private final FriendRequestService friendRequestService;
    private final FileUtil fileUtil;
    /**
     * ID로 일기를 조회하여 다른 서비스에서 사용할 수 있도록 반환합니다.
     *
     * @param diaryId 조회할 일기의 ID
     * @return 조회된 Diary 엔티티
     * @throws DiaryNotFoundException 해당 ID의 일기가 존재하지 않을 경우
     */
    public Diary getDiaryById(Long diaryId) {
        return diaryRepository.findById(diaryId)
                .orElseThrow(() -> {
                    log.error("일기 ID [{}]에 해당하는 일기를 찾을 수 없습니다.", diaryId);
                    return new DiaryNotFoundException();
                });
    }

    @Transactional
    public ResponseDiaryDTO createDiary(DiaryDTO diaryDTO, String userId) throws UserNotFoundException {

        validateDiaryDTO(diaryDTO);


        log.info("사용자 조회");
        User user = userService.getUserById(userId);

        Optional<Diary> existingDiary = diaryRepository.findByUserAndDate(user, diaryDTO.getDate());

        if (existingDiary.isPresent()) {
            log.info("일기 날짜 중복 요청");
            throw new DuplicateDiaryException("이미 해당 날짜에 일기가 존재합니다: " + diaryDTO.getDate());
        }

        Diary diary = new Diary(diaryDTO.getContent(), diaryDTO.getStatus(),diaryDTO.getDate(), user);
        diary = diaryRepository.save(diary);
        log.info("사용자 [{}] - 일기 저장 완료. 일기 ID: {}", userId, diary.getId());



        List<MultipartFile> photos = diaryDTO.getPhotos();
        List<Long> aiPhotos = diaryDTO.getAiPhotos();

        try {
            int photoCoverIndex = -1;
            int aiCoverIndex = -1;

            if (DiaryPhotoType.AI_IMAGE == diaryDTO.getCoverPhotoType()) {
                aiCoverIndex = diaryDTO.getCoverPhotoIndex();
            } else {
                photoCoverIndex = diaryDTO.getCoverPhotoIndex();
            }

            if (photos != null ) {
                photoStorage.savePhoto(diary, photos, userId, photoCoverIndex);
            }

            if (aiPhotos != null) {
                photoStorage.saveAiPhoto(diary, aiPhotos, userId, aiCoverIndex);
            }
            log.info("사용자 [{}] - 사진 저장 완료. 사진 개수: {}", userId, photos.size());
        } catch (IOException e) {
            log.error("사진 저장 실패 : {}", e.getMessage(), e);
            throw new RuntimeException("사진 저장 실패", e); // 트랜잭션 롤백
        }
        return new ResponseDiaryDTO(
                diary.getId(),
                diary.getContent()
        );

    }

    private void validateDiaryDTO(DiaryDTO diaryDTO) {
        List list;
        if (DiaryPhotoType.AI_IMAGE == diaryDTO.getCoverPhotoType()){
           list = diaryDTO.getAiPhotos();
        } else {
          list = diaryDTO.getPhotos();
        }
        validateCoverPhoto(list, diaryDTO.getCoverPhotoIndex());
        validatePhotos(diaryDTO.getPhotos());
    }

    private void validatePhotos(List<MultipartFile> photos) {
        for (MultipartFile file : photos) {
            String originalFilename = file.getOriginalFilename();

            if (originalFilename == null || !originalFilename.contains(".")) {
                throw new IllegalArgumentException("유효하지 않은 파일 이름입니다: " + originalFilename);
            }

            String contentType = fileUtil.getContentType(originalFilename);

            // 허용할 이미지 타입 목록
            List<String> allowedImageTypes = List.of(
                    "image/jpeg",
                    "image/heic",
                    "image/png",
                    "image/gif",
                    "image/webp",
                    "image/bmp",
                    "image/svg+xml"
            );

            if (!allowedImageTypes.contains(contentType)) {
                throw new IllegalArgumentException("허용되지 않는 이미지 확장자입니다: " + originalFilename);
            }
        }
    }

    private void validateCoverPhoto(List list, int coverPhotoIndex){
        if (list == null || list.isEmpty()) {
            log.error("사진 리스트가 비어있습니다.");
            throw new IllegalArgumentException("사진 리스트가 비어있습니다.");
        }
        if (coverPhotoIndex < 0 || coverPhotoIndex >= list.size()) {
            log.error("유효하지 않은 커버 사진 인덱스: {}", coverPhotoIndex);
            throw new IllegalArgumentException("유효하지 않은 커버 사진 인덱스: " + coverPhotoIndex);
        }
    }


    @Transactional(readOnly = true)
    public ResponseDTO getDiaryWithPhotos(Long diaryId, RequestMetaInfo requestMetaInfo,String user_id) {
        log.info("{} 일기 내용 조회 요청", diaryId);
        Diary diary = getDiaryById(diaryId);

        // 모든 사진 리스트 한 번만 조회
        List<Photo> photos = photoRepository.findByDiaryId(diary.getId());
//        if (photos == null || photos.isEmpty()) {
//            log.warn("DiaryId {} 에 해당하는 사진이 없음!", diaryId);
//            throw new DiaryNotFoundException();
//        }

        List<String> sortedPhotoUrls = sortPhotos(photos,requestMetaInfo);
        boolean isOwner = diary.getUser().getId().equals(user_id);
        boolean isFriend = friendRequestService.areFriends(diary.getUser().getId(), user_id);

        String avatarUrl = imagePathToUrlConverter.userAvatarImageUrl(diary.getUser().getAvatar(), requestMetaInfo);

        // 비공개 + 본인 아님 → 대표 사진만 반환
        if ((diary.getStatus() == Status.PRIVATE && !isOwner)
                || (diary.getStatus() == Status.FRIENDS && !isOwner && !isFriend)) {
            return new ResponseDTO(
                    diary.getId(),
                    diary.getStatus(),
                    null,
                    List.of(sortedPhotoUrls.get(0)),
                    diary.getDate(),
                    diary.getUser().getNickname(),
                    avatarUrl,
                    diary.getUser().getId(),
                    diary.getCreatedAt(),
                    null
            );
        }

        // 공개이거나 본인일 경우 대표 사진 포함 전체 사진 리스트 반환
        return new ResponseDTO(
                diary.getId(),
                diary.getStatus(),
                diary.getContent(),
                sortedPhotoUrls,
                diary.getDate(),
                diary.getUser().getNickname(),
                avatarUrl,
                diary.getUser().getId(),
                diary.getCreatedAt(),
                null
        );
    }


    public List<CalendarDiaryResponseDTO> findMonthlyDiaries(String userId, int year, int month, RequestMetaInfo requestMetaInfo) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startOfMonth = yearMonth.atDay(1);
        LocalDate endOfMonth = yearMonth.atEndOfMonth();

        List<Diary> diaries = diaryRepository.findByUserIdAndDateBetween(userId, startOfMonth, endOfMonth);

        return diaries.stream().map(diary -> {
            String coverPhotoUrl = photoRepository.findFirstByDiaryIdAndRepresentIsTrue(diary.getId())
                    .map(Photo::getUrl)
                    .map(url -> imagePathToUrlConverter.diaryImageUrl(url, requestMetaInfo))
                    .orElse(null); // 대표 이미지가 없는 경우 null 처리, 혹은 기본 이미지 URL 설정
            return new CalendarDiaryResponseDTO(diary.getId(), coverPhotoUrl, diary.getDate());
        }).collect(Collectors.toList());
    }


    private List<String> sortPhotos(List<Photo> photos,RequestMetaInfo requestMetaInfo ) {
        for (int i = 0; i < photos.size(); i++) {
            if (Boolean.TRUE.equals(photos.get(i).getRepresent())) {
                if (i != 0) {
                    Photo representPhoto = photos.remove(i);
                    photos.add(0, representPhoto);
                }
                break;
            }
        }

        return photos.stream()
                .map(photo -> imagePathToUrlConverter.diaryImageUrl(photo.getUrl(), requestMetaInfo))
                .toList();
    }




    /**
     * 공개 상태인 일기들을 페이지네이션과 함께 조회하고,
     * 각 일기별 대표 사진이 앞에 오도록 사진 URL 리스트를 정렬하여 반환합니다.
     *
     * @param pageable 조회할 페이지 번호 (0부터 시작)
     * @return 공개된 일기 리스트의 DTO를 담은 Page
     */
    public Page<ResponseDTO> getAllDiaries(Pageable pageable ,RequestMetaInfo requestMetaInfo,String user_id) {
        Page<Diary> page = diaryRepository.findByStatus(Status.PUBLIC, pageable);

        return page.map(diary -> {
            List<Photo> photos = photoRepository.findByDiaryId(diary.getId());
            List<String> sortedPhotoUrls = sortPhotos(photos,requestMetaInfo);
            String avatarUrl = imagePathToUrlConverter.userAvatarImageUrl(diary.getUser().getAvatar(), requestMetaInfo);
            FriendStatus friendshipStatus = friendRequestService.getFriendshipStatus(user_id, diary.getUser().getId());

            return new ResponseDTO(
                    diary.getId(),
                    diary.getStatus(),
                    diary.getContent(),
                    sortedPhotoUrls,
                    diary.getDate(),
                    diary.getUser().getNickname(),
                    avatarUrl,
                    diary.getUser().getId(),
                    diary.getCreatedAt(),
                    friendshipStatus
            );
        });
    }
}
