package store.piku.back.diary.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import store.piku.back.ai.entity.DiaryImageGeneration;
import store.piku.back.ai.repository.DiaryImageGenerationRepository;
import store.piku.back.ai.service.DiaryImageGenerationService;
import store.piku.back.diary.dto.*;
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
import store.piku.back.notification.entity.NotificationType;
import store.piku.back.notification.service.NotificationService;
import store.piku.back.user.entity.User;
import store.piku.back.user.exception.UserNotFoundException;
import store.piku.back.user.service.reader.UserReader;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@Slf4j
@RequiredArgsConstructor
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final PhotoRepository photoRepository;
    private final UserReader userReader;
    private final PhotoStorageService photoStorage;
    private final ImagePathToUrlConverter imagePathToUrlConverter;
    private final FriendRequestService friendRequestService;
    private final FileUtil fileUtil;
    private final DiaryImageGenerationRepository diaryImageGenerationRepository;
    private final DiaryImageGenerationService diaryImageGenerationService;
    private final NotificationService notificationService;

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
    public ResponseDiaryDTO createDiary(DiaryDTO diaryDTO, List<MultipartFile> photos, String userId) throws UserNotFoundException, IOException {

        validateDiaryDTO(diaryDTO, photos, userId);

        User user = userReader.getUserById(userId);

        Diary diary = new Diary(diaryDTO.getContent(), diaryDTO.getStatus(),diaryDTO.getDate(), user);

        diary = diaryRepository.save(diary);
        log.debug("사용자 [{}] - 일기 저장 완료. 일기 ID: {}", userId, diary.getId());

        List<DiaryImageInfo> infos = diaryDTO.getImageInfos();
        infos.sort(Comparator.comparing(DiaryImageInfo::getOrder));

        // imageInfos를 순회하면서 하나씩 저장
        for(DiaryImageInfo info : infos) {
            if (info.getType() == DiaryPhotoType.AI_IMAGE) {
                saveAiPhoto(diary, info.getAiPhotoId(), userId, info.getOrder());
            } else {
                if (info.getPhotoIndex() != null && photos != null && info.getPhotoIndex() < photos.size()) {
                    photoStorage.savePhoto(diary, photos.get(info.getPhotoIndex()), userId, info.getOrder());
                }
            }
        }
        log.debug("사용자 [{}] - 사진 저장 완료. 일기 ID: {}", userId, diary.getId());

        if (diary.getStatus() == Status.FRIENDS) {
            List<String> friends = friendRequestService.getFriends(userId);
            for (String friendId : friends) {

                if (friendId.equals(userId)) continue;

                notificationService.sendNotification(
                        friendId,
                        NotificationType.FRIEND_DIARY,
                        user.getId(),
                        diary
                );
            }
            log.info("친구에게 새 일기 공개 알림 전송 완료. 친구 수: {}", friends.size());

        }
        return new ResponseDiaryDTO(
                diary.getId(),
                diary.getContent()
        );
    }

    public void saveAiPhoto(Diary diary, Long aiPhoto, String userId, Integer order) {
        log.info("AI 사진 저장 시작 - 사용자: {}, 일기 날짜: {}", userId, diary.getDate());

        if (aiPhoto != null) {
            DiaryImageGeneration diaryImageGeneration = diaryImageGenerationService.findById(aiPhoto);
            String filePath = diaryImageGeneration.getFilePath();

            Photo savePhoto = new Photo(diary, filePath, order);
            if (order == 0) {
                savePhoto.updateRepresent(true); // 첫 번째 AI 사진을 대표 사진으로 설정
            }
            photoRepository.save(savePhoto);
            diaryImageGenerationService.updateDiaryId(aiPhoto, diary.getId());
        } else {
            log.warn("빈 AI 사진 ID 발견 - 사용자: {}, 일기 날짜: {}", userId, diary.getDate());
        }
    }

    // @Transactional
    // public ResponseDiaryDTO createDiary(DiaryDTO diaryDTO, String userId) throws UserNotFoundException {
    //
    //     validateDiaryDTO(diaryDTO);
    //
    //
    //     log.info("사용자 조회");
    //     User user = userReader.getUserById(userId);
    //
    //     Optional<Diary> existingDiary = diaryRepository.findByUserAndDate(user, diaryDTO.getDate());
    //
    //     if (existingDiary.isPresent()) {
    //         log.info("일기 날짜 중복 요청");
    //         throw new DuplicateDiaryException("이미 해당 날짜에 일기가 존재합니다: " + diaryDTO.getDate());
    //     }
    //
    //     Diary diary = new Diary(diaryDTO.getContent(), diaryDTO.getStatus(),diaryDTO.getDate(), user);
    //     diary = diaryRepository.save(diary);
    //     log.info("사용자 [{}] - 일기 저장 완료. 일기 ID: {}", userId, diary.getId());
    //
    //
    //
    //     List<MultipartFile> photos = diaryDTO.getPhotos();
    //     List<Long> aiPhotos = diaryDTO.getAiPhotos();
    //
    //     try {
    //         int photoCoverIndex = -1;
    //         int aiCoverIndex = -1;
    //
    //         if (DiaryPhotoType.AI_IMAGE == diaryDTO.getCoverPhotoType()) {
    //             aiCoverIndex = diaryDTO.getCoverPhotoIndex();
    //         } else {
    //             photoCoverIndex = diaryDTO.getCoverPhotoIndex();
    //         }
    //
    //         if (photos != null && !photos.isEmpty()) {
    //             photoStorage.savePhoto(diary, photos, userId, photoCoverIndex);
    //             log.info("사용자 [{}] - 사진 저장 완료. 사진 개수: {}", userId, photos.size());
    //         }
    //
    //         if (aiPhotos != null && !aiPhotos.isEmpty()) {
    //             photoStorage.saveAiPhoto(diary, aiPhotos, userId, aiCoverIndex);
    //         }
    //     } catch (IOException e) {
    //         log.error("사진 저장 실패 : {}", e.getMessage(), e);
    //         throw new RuntimeException("사진 저장 실패", e); // 트랜잭션 롤백
    //     }
    //     return new ResponseDiaryDTO(
    //             diary.getId(),
    //             diary.getContent()
    //     );
    //
    // }

    // private void validateDiaryDTO(DiaryDTO diaryDTO) {
    //     List list;
    //     if (DiaryPhotoType.AI_IMAGE == diaryDTO.getCoverPhotoType()){
    //        list = diaryDTO.getAiPhotos();
    //     } else {
    //       list = diaryDTO.getPhotos();
    //     }
    //     validateCoverPhoto(list, diaryDTO.getCoverPhotoIndex());
    //     validatePhotos(diaryDTO.getPhotos());
    // }

    private void validateDiaryDate(DiaryDTO diaryDTO, String userId) {
        User user = userReader.getUserById(userId);
        Optional<Diary> existingDiary = diaryRepository.findByUserAndDate(user, diaryDTO.getDate());
        if (existingDiary.isPresent()) {
            log.info("일기 날짜 중복 요청");
            throw new DuplicateDiaryException("이미 해당 날짜에 일기가 존재합니다: " + diaryDTO.getDate());
        }
        LocalDate localDate = LocalDate.now();
        if (diaryDTO.getDate().isAfter(localDate)) {
            log.error("미래 날짜에 일기 작성 시도: {}", diaryDTO.getDate());
            throw new IllegalArgumentException("미래 날짜에 일기를 작성할 수 없습니다: " + diaryDTO.getDate());
        }
    }

    private void validateDiaryDTO(DiaryDTO diaryDTO, List<MultipartFile> photos, String userId) {
        validatePhotos(photos);
        validateDiaryDate(diaryDTO, userId);


        List<DiaryImageInfo> infos = diaryDTO.getImageInfos();
        infos.sort(Comparator.comparing(DiaryImageInfo::getOrder));
        Set<Integer> uniqueOrders = infos.stream()
                .map(DiaryImageInfo::getOrder)
                .collect(Collectors.toSet());
        if (uniqueOrders.size() != infos.size()) {
            throw new IllegalArgumentException("이미지 순서가 중복되었습니다.");
        }
        int userImageCount = 0;
        for (DiaryImageInfo info : infos) {
            if (info.getType() == DiaryPhotoType.AI_IMAGE){
                if (!diaryImageGenerationRepository.existsByIdAndUserId(info.getAiPhotoId(), userId)){
                    throw new IllegalArgumentException("유효하지 않은 AI 사진 ID: " + info.getAiPhotoId());
                }
            }
            if (info.getType() == DiaryPhotoType.USER_IMAGE){
                if (info.getPhotoIndex() == null){
                    throw new IllegalArgumentException("유효하지 않은 사용자 사진 인덱스: null");
                }
                userImageCount++;
            }
        }
        if (userImageCount != (photos == null ? 0 : photos.size())) {
            throw new IllegalArgumentException("사용자 사진 개수와 이미지 정보 개수가 일치하지 않습니다.");
        }
    }

    private void validatePhotos(List<MultipartFile> photos) {
        if (photos == null || photos.isEmpty()) {
            return;
        }
        for (MultipartFile file : photos) {
            String originalFilename = file.getOriginalFilename();

            if (originalFilename == null || !originalFilename.contains(".")) {
                throw new IllegalArgumentException("유효하지 않은 파일 이름입니다: " + originalFilename);
            }

            String contentType = fileUtil.getContentType(originalFilename);

            // 허용할 이미지 타입 목록
            List<String> allowedImageTypes = List.of(
                    "image/jpeg",
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
                    .map(photoStorage::getPhotoUrl)
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
                .map(photo -> photoStorage.getPhotoUrl(photo.getUrl()))
                .toList();
    }

    public Pageable sanitizePageable(Pageable pageable, List<String> allowedSortFields) {
        int page = Math.max(pageable.getPageNumber(), 0);
        int size = Math.min(Math.max(pageable.getPageSize(), 1), 100);

        Sort safeSort = Sort.unsorted();

        for (Sort.Order order : pageable.getSort()) {
            String property = order.getProperty();
            if (allowedSortFields.contains(property)) {
                safeSort = safeSort.and(Sort.by(order));
            } else {
                log.warn("정렬 필드 '{}' 은 허용되지 않았습니다. 무시됩니다.", property);
            }
        }
        if (!safeSort.isSorted()) {
            safeSort = Sort.by(Sort.Direction.DESC, "createdAt");
        }

        return PageRequest.of(page, size, safeSort);
    }


    /**
     * 공개 상태인 일기들을 페이지네이션과 함께 조회하고,
     * 각 일기별 대표 사진이 앞에 오도록 사진 URL 리스트를 정렬하여 반환합니다.
     *
     * @param pageable 조회할 페이지 번호 (0부터 시작)
     * @return 공개된 일기 리스트의 DTO를 담은 Page
     */
    public Page<ResponseDTO> getAllDiaries(Pageable pageable ,RequestMetaInfo requestMetaInfo,String user_id) {

        List<String> allowed = List.of("createdAt");
        Pageable safePageable = sanitizePageable(pageable,allowed);
        List<String> friendIds = friendRequestService.findFriendIdList(safePageable,user_id,requestMetaInfo);

        Page<Diary> page = diaryRepository.findFeedByFriendIdsOrPublic(friendIds, safePageable);


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

    /**
     * 특정 사용자가 작성한 일기의 총 개수를 반환합니다.
     *
     * @param userId 일기 개수를 조회할 사용자의 ID
     * @return 해당 사용자가 작성한 일기의 총 개수
     */
    @Transactional(readOnly = true)
    public long countDiariesByUserId(String userId) {
        log.info("사용자 ID: {} 의 일기 개수 조회 요청", userId);

        return diaryRepository.countByUserId(userId);
    }


    /**
     * 해당 프로필 사용자의 월별 일기 수를 반환합니다.
     *
     * @param profileId
     * @return "2025-06": 4,
     *     "2025-07": 13
     */
    public List<DiaryMonthCountDTO> getMonthlyDiaryCount(String profileId) {
        LocalDate monthsAgo = LocalDate.now().minusMonths(6).withDayOfMonth(1);
        List<DiaryMonthCountDTO> counts = diaryRepository.countDiariesPerMonth(profileId,monthsAgo);

        return counts;
    }
}
