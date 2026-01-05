package store.piku.back.diary.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.piku.back.comment.service.CommentService;
import store.piku.back.diary.dto.ResponseDTO;
import store.piku.back.diary.entity.Diary;
import store.piku.back.diary.entity.FeedClick;
import store.piku.back.diary.entity.Photo;
import store.piku.back.diary.enums.FriendStatus;
import store.piku.back.diary.enums.Status;
import store.piku.back.diary.repository.DiaryRepository;
import store.piku.back.diary.repository.FeedClickRepository;
import store.piku.back.diary.repository.PhotoRepository;
import store.piku.back.friend.service.FriendRequestService;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.global.util.ImagePathToUrlConverter;
import store.piku.back.like.service.LikeService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final FeedClickRepository feedClickRepository;
    private final LikeService likeService;

    @Transactional(readOnly = true)
    public ResponseDTO getDiaryWithPhotos(Long diaryId, RequestMetaInfo requestMetaInfo, String user_id) {
        log.info("{} 일기 내용 조회 요청", diaryId);
        Diary diary = diaryService.getDiaryById(diaryId);

        List<Photo> photos = photoRepository.findByDiaryId(diary.getId());

        List<String> sortedPhotoUrls = diaryService.sortPhotos(photos, requestMetaInfo);
        boolean isOwner = diary.getUser().getId().equals(user_id);
        boolean isFriend = friendRequestService.areFriends(diary.getUser().getId(), user_id);

        String avatarUrl = imagePathToUrlConverter.userAvatarImageUrl(diary.getUser().getAvatar(), requestMetaInfo);

        long likeCount = likeService.getLikeCount(diaryId);
        boolean isLiked = likeService.isLikedByUser(user_id, diaryId);

        // 비공개 + 본인 아님 → 대표 사진만 반환
        if ((diary.getStatus() == Status.PRIVATE && !isOwner)
                || (diary.getStatus() == Status.FRIENDS && !isOwner && !isFriend)) {
            return ResponseDTO.builder()
                    .diaryId(diary.getId())
                    .status(diary.getStatus())
                    .content(null)
                    .imgUrls(List.of(sortedPhotoUrls.get(0)))
                    .date(diary.getDate())
                    .nickname(diary.getUser().getNickname())
                    .avatar(avatarUrl)
                    .userId(diary.getUser().getId())
                    .createdAt(diary.getCreatedAt())
                    .friendStatus(null)
                    .commentCount(commentService.countAllCommentsByDiaryId(diary.getId()))
                    .likeCount(likeCount)
                    .isLiked(isLiked)
                    .build();
        }

        // 공개이거나 본인일 경우 대표 사진 포함 전체 사진 리스트 반환
        return ResponseDTO.builder()
                .diaryId(diary.getId())
                .status(diary.getStatus())
                .content(diary.getContent())
                .imgUrls(sortedPhotoUrls)
                .date(diary.getDate())
                .nickname(diary.getUser().getNickname())
                .avatar(avatarUrl)
                .userId(diary.getUser().getId())
                .createdAt(diary.getCreatedAt())
                .friendStatus(null)
                .commentCount(commentService.countAllCommentsByDiaryId(diary.getId()))
                .likeCount(likeCount)
                .isLiked(isLiked)
                .build();
    }

    /**
     * 공개 상태인 일기들을 페이지네이션과 함께 조회하고,
     * 각 일기별 대표 사진이 앞에 오도록 사진 URL 리스트를 정렬하여 반환합니다.
     *
     * @param pageable 조회할 페이지 번호 (0부터 시작)
     * @return 공개된 일기 리스트의 DTO를 담은 Page
     */
    public Page<ResponseDTO> getAllDiaries(Pageable pageable, RequestMetaInfo requestMetaInfo, String user_id) {

        List<String> allowed = List.of("createdAt");
        Pageable safePageable = diaryService.sanitizePageable(pageable, allowed);

        List<Diary> unreadFriendFeeds = Collections.emptyList();
        List<Long> clickedFeedIds = Collections.emptyList();
        List<Diary> recentClickedFeeds = Collections.emptyList();

        if (user_id != null) {
            List<String> friendIds = friendRequestService.findFriendIdList(safePageable, user_id, requestMetaInfo);
            clickedFeedIds = feedClickRepository.findClickedDiaryIdsByUserId(user_id);
            LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);

            unreadFriendFeeds = diaryRepository.findUnreadFeedsByVisibilityAndUserIds(
                    Status.FRIENDS, friendIds, clickedFeedIds);

            recentClickedFeeds = diaryRepository.findClickedFeedsAfter(clickedFeedIds, threeDaysAgo);
            Collections.shuffle(recentClickedFeeds);
        }

        // user_id 가 null이든 아니든 전체공개는 항상 가능
        List<Diary> unreadPublicFeeds = diaryRepository.findUnreadPublicFeeds(clickedFeedIds);

        // 우선순위대로 합침
        List<Diary> combined = new ArrayList<>();
        combined.addAll(unreadFriendFeeds);
        combined.addAll(unreadPublicFeeds);
        combined.addAll(recentClickedFeeds);

        int total = combined.size();

        // 페이징
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), total);
        List<Diary> pagedDiaries = start >= total ? Collections.emptyList() : combined.subList(start, end);

        // 좋아요 정보 배치 조회
        List<Long> diaryIds = pagedDiaries.stream().map(Diary::getId).collect(Collectors.toList());
        Map<Long, Long> likeCountMap = likeService.getLikeCountsForDiaries(diaryIds);
        Set<Long> likedDiaryIds = likeService.getLikedDiaryIds(user_id, diaryIds);

        List<ResponseDTO> responseList = pagedDiaries.stream().map(diary -> {
            List<Photo> photos = photoRepository.findByDiaryId(diary.getId());
            List<String> sortedPhotoUrls = diaryService.sortPhotos(photos, requestMetaInfo);
            String avatarUrl = imagePathToUrlConverter.userAvatarImageUrl(diary.getUser().getAvatar(), requestMetaInfo);

            FriendStatus friendshipStatus = FriendStatus.NONE;
            if (user_id != null) {
                friendshipStatus = friendRequestService.getFriendshipStatus(user_id, diary.getUser().getId());
            }

            return ResponseDTO.builder()
                    .diaryId(diary.getId())
                    .status(diary.getStatus())
                    .content(diary.getContent())
                    .imgUrls(sortedPhotoUrls)
                    .date(diary.getDate())
                    .nickname(diary.getUser().getNickname())
                    .avatar(avatarUrl)
                    .userId(diary.getUser().getId())
                    .createdAt(diary.getCreatedAt())
                    .friendStatus(friendshipStatus)
                    .commentCount(commentService.countAllCommentsByDiaryId(diary.getId()))
                    .likeCount(likeCountMap.getOrDefault(diary.getId(), 0L))
                    .isLiked(likedDiaryIds.contains(diary.getId()))
                    .build();
        }).collect(Collectors.toList());

        return new PageImpl<>(responseList, pageable, total);
    }

    public void logClick(String userId, Long diaryId) {

        boolean alreadyClicked = feedClickRepository.existsByUserIdAndDiaryId(userId, diaryId);
        if (alreadyClicked) {
            return;
        }
        FeedClick click = new FeedClick(userId, diaryId);
        feedClickRepository.save(click);
    }

}
