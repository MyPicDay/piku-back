package store.piku.back.diary.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.piku.back.comment.service.CommentService;
import store.piku.back.diary.dto.ResponseDTO;
import store.piku.back.diary.entity.Diary;
import store.piku.back.diary.entity.Photo;
import store.piku.back.diary.enums.FriendStatus;
import store.piku.back.diary.enums.Status;
import store.piku.back.diary.repository.DiaryRepository;
import store.piku.back.diary.repository.PhotoRepository;
import store.piku.back.friend.service.FriendRequestService;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.global.util.ImagePathToUrlConverter;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FeedService {


    private final DiaryService diaryService;
    private final CommentService commentService;
    private final PhotoRepository photoRepository;
    private final DiaryRepository diaryRepository;
    private final ImagePathToUrlConverter imagePathToUrlConverter;
    private final FriendRequestService friendRequestService;


    @Transactional(readOnly = true)
    public ResponseDTO getDiaryWithPhotos(Long diaryId, RequestMetaInfo requestMetaInfo, String user_id) {
        log.info("{} 일기 내용 조회 요청", diaryId);
        Diary diary = diaryService.getDiaryById(diaryId);

        List<Photo> photos = photoRepository.findByDiaryId(diary.getId());
//        if (photos == null || photos.isEmpty()) {
//            log.warn("DiaryId {} 에 해당하는 사진이 없음!", diaryId);
//            throw new DiaryNotFoundException();
//        }

        List<String> sortedPhotoUrls = diaryService.sortPhotos(photos,requestMetaInfo);
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
                    null,
                    commentService.countAllCommentsByDiaryId(diary.getId())
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
                null,
                commentService.countAllCommentsByDiaryId(diary.getId())
        );
    }

    /**
     * 공개 상태인 일기들을 페이지네이션과 함께 조회하고,
     * 각 일기별 대표 사진이 앞에 오도록 사진 URL 리스트를 정렬하여 반환합니다.
     *
     * @param pageable 조회할 페이지 번호 (0부터 시작)
     * @return 공개된 일기 리스트의 DTO를 담은 Page
     */
    public Page<ResponseDTO> getAllDiaries(Pageable pageable , RequestMetaInfo requestMetaInfo, String user_id) {

        List<String> allowed = List.of("createdAt");
        Pageable safePageable = diaryService.sanitizePageable(pageable,allowed);
        List<String> friendIds = friendRequestService.findFriendIdList(safePageable,user_id,requestMetaInfo);

        Page<Diary> page = diaryRepository.findFeedByFriendIdsOrPublic(friendIds, safePageable);


        return page.map(diary -> {
            List<Photo> photos = photoRepository.findByDiaryId(diary.getId());
            List<String> sortedPhotoUrls = diaryService.sortPhotos(photos,requestMetaInfo);
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
                    friendshipStatus,
                    commentService.countAllCommentsByDiaryId(diary.getId())
            );
        });
    }



}
