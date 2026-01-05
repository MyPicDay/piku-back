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
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FeedService {

    private static final int RECENT_CLICK_DAYS = 3;

    private final DiaryService diaryService;
    private final CommentService commentService;
    private final PhotoRepository photoRepository;
    private final DiaryRepository diaryRepository;
    private final ImagePathToUrlConverter imagePathToUrlConverter;
    private final FriendRequestService friendRequestService;
    private final FeedClickRepository feedClickRepository;
    private final LikeService likeService;

    @Transactional(readOnly = true)
    public ResponseDTO getDiaryWithPhotos(Long diaryId, RequestMetaInfo requestMetaInfo, String userId) {
        log.info("일기 상세 조회 요청 - diaryId: {}", diaryId);

        Diary diary = diaryService.getDiaryById(diaryId);
        List<String> photoUrls = getPhotosForDiary(diary, requestMetaInfo);

        boolean isOwner = diary.getUser().getId().equals(userId);
        boolean isFriend = friendRequestService.areFriends(diary.getUser().getId(), userId);
        boolean hasAccess = isOwner || (diary.getStatus() == Status.PUBLIC)
                || (diary.getStatus() == Status.FRIENDS && isFriend);

        return buildResponseDTO(diary, photoUrls, requestMetaInfo, userId, null, hasAccess);
    }

    @Transactional(readOnly = true)
    public Page<ResponseDTO> getAllDiaries(Pageable pageable, RequestMetaInfo requestMetaInfo, String userId) {
        Pageable safePageable = diaryService.sanitizePageable(pageable, List.of("createdAt"));

        List<Diary> feedCandidates = collectFeedCandidates(safePageable, userId, requestMetaInfo);
        List<Diary> pagedDiaries = applyPaging(feedCandidates, pageable);

        Map<Long, Long> likeCountMap = getLikeCountsForDiaries(pagedDiaries);
        Set<Long> likedDiaryIds = getLikedDiaryIds(pagedDiaries, userId);

        List<ResponseDTO> responseList = pagedDiaries.stream()
                .map(diary -> buildResponseDTOForFeed(diary, requestMetaInfo, userId, likeCountMap, likedDiaryIds))
                .collect(Collectors.toList());

        return new PageImpl<>(responseList, pageable, feedCandidates.size());
    }

    @Transactional
    public void logClick(String userId, Long diaryId) {
        if (feedClickRepository.existsByUserIdAndDiaryId(userId, diaryId)) {
            return;
        }
        feedClickRepository.save(new FeedClick(userId, diaryId));
    }

    private List<Diary> collectFeedCandidates(Pageable pageable, String userId, RequestMetaInfo requestMetaInfo) {
        List<Long> clickedFeedIds = Collections.emptyList();
        List<Diary> unreadFriendFeeds = Collections.emptyList();
        List<Diary> recentClickedFeeds = Collections.emptyList();

        if (userId != null) {
            List<String> friendIds = friendRequestService.findFriendIdList(pageable, userId, requestMetaInfo);
            clickedFeedIds = feedClickRepository.findClickedDiaryIdsByUserId(userId);
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(RECENT_CLICK_DAYS);

            unreadFriendFeeds = diaryRepository.findUnreadFeedsByVisibilityAndUserIds(
                    Status.FRIENDS, friendIds, clickedFeedIds);
            recentClickedFeeds = new ArrayList<>(diaryRepository.findClickedFeedsAfter(clickedFeedIds, cutoffDate));
            Collections.shuffle(recentClickedFeeds);
        }

        List<Diary> unreadPublicFeeds = diaryRepository.findUnreadPublicFeeds(clickedFeedIds);

        List<Diary> combined = new ArrayList<>();
        combined.addAll(unreadFriendFeeds);
        combined.addAll(unreadPublicFeeds);
        combined.addAll(recentClickedFeeds);

        return combined;
    }

    private List<Diary> applyPaging(List<Diary> diaries, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), diaries.size());
        return start >= diaries.size() ? Collections.emptyList() : diaries.subList(start, end);
    }

    private List<String> getPhotosForDiary(Diary diary, RequestMetaInfo requestMetaInfo) {
        List<Photo> photos = photoRepository.findByDiaryId(diary.getId());
        return diaryService.sortPhotos(photos, requestMetaInfo);
    }

    private ResponseDTO buildResponseDTO(Diary diary, List<String> photoUrls, RequestMetaInfo requestMetaInfo,
            String userId, FriendStatus friendStatus, boolean hasFullAccess) {
        String avatarUrl = imagePathToUrlConverter.userAvatarImageUrl(diary.getUser().getAvatar(), requestMetaInfo);
        long likeCount = likeService.getLikeCount(diary.getId());
        boolean isLiked = likeService.isLikedByUser(userId, diary.getId());

        List<String> displayPhotos = hasFullAccess ? photoUrls : List.of(photoUrls.get(0));
        String displayContent = hasFullAccess ? diary.getContent() : null;

        return ResponseDTO.builder()
                .diaryId(diary.getId())
                .status(diary.getStatus())
                .content(displayContent)
                .imgUrls(displayPhotos)
                .date(diary.getDate())
                .nickname(diary.getUser().getNickname())
                .avatar(avatarUrl)
                .userId(diary.getUser().getId())
                .createdAt(diary.getCreatedAt())
                .friendStatus(friendStatus)
                .commentCount(commentService.countAllCommentsByDiaryId(diary.getId()))
                .likeCount(likeCount)
                .isLiked(isLiked)
                .build();
    }

    private ResponseDTO buildResponseDTOForFeed(Diary diary, RequestMetaInfo requestMetaInfo, String userId,
            Map<Long, Long> likeCountMap, Set<Long> likedDiaryIds) {
        List<String> photoUrls = getPhotosForDiary(diary, requestMetaInfo);
        String avatarUrl = imagePathToUrlConverter.userAvatarImageUrl(diary.getUser().getAvatar(), requestMetaInfo);

        FriendStatus friendStatus = userId != null
                ? friendRequestService.getFriendshipStatus(userId, diary.getUser().getId())
                : FriendStatus.NONE;

        return ResponseDTO.builder()
                .diaryId(diary.getId())
                .status(diary.getStatus())
                .content(diary.getContent())
                .imgUrls(photoUrls)
                .date(diary.getDate())
                .nickname(diary.getUser().getNickname())
                .avatar(avatarUrl)
                .userId(diary.getUser().getId())
                .createdAt(diary.getCreatedAt())
                .friendStatus(friendStatus)
                .commentCount(commentService.countAllCommentsByDiaryId(diary.getId()))
                .likeCount(likeCountMap.getOrDefault(diary.getId(), 0L))
                .isLiked(likedDiaryIds.contains(diary.getId()))
                .build();
    }

    private Map<Long, Long> getLikeCountsForDiaries(List<Diary> diaries) {
        List<Long> diaryIds = diaries.stream().map(Diary::getId).collect(Collectors.toList());
        return likeService.getLikeCountsForDiaries(diaryIds);
    }

    private Set<Long> getLikedDiaryIds(List<Diary> diaries, String userId) {
        List<Long> diaryIds = diaries.stream().map(Diary::getId).collect(Collectors.toList());
        return likeService.getLikedDiaryIds(userId, diaryIds);
    }
}
