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
import store.piku.back.recommendation.service.FeedCompositionService;
import store.piku.back.recommendation.service.RecommendationCacheService;
import store.piku.back.recommendation.service.UserPreferenceService;
import store.piku.back.recommendation.service.DiaryMetadataService;

import java.time.LocalDateTime;
import java.util.*;
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
    private final FeedCompositionService feedCompositionService;
    private final RecommendationCacheService recommendationCacheService;
    private final UserPreferenceService userPreferenceService;
    private final DiaryMetadataService diaryMetadataService;

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

        // 1. 캐시 확인
        List<Long> cachedDiaryIds = userId != null
                ? recommendationCacheService.getCachedFeed(userId)
                : Collections.emptyList();

        List<Diary> pagedDiaries;
        int totalSize;

        if (!cachedDiaryIds.isEmpty()) {
            // 캐시 히트: 캐시된 ID로 직접 조회
            log.info("피드 캐시 히트 - userId: {}", userId);
            List<Diary> cachedDiaries = diaryRepository.findAllById(cachedDiaryIds);
            pagedDiaries = applyPaging(cachedDiaries, pageable);
            totalSize = cachedDiaries.size();
        } else {
            // 캐시 미스: 추천 시스템 사용
            log.info("피드 캐시 미스 - userId: {}", userId);
            List<Diary> feedCandidates = collectFeedCandidates(safePageable, userId, requestMetaInfo);

            // 추천 스코어링 적용
            List<Diary> scoredDiaries = applyRecommendationScoring(feedCandidates, userId, requestMetaInfo);
            pagedDiaries = applyPaging(scoredDiaries, pageable);
            totalSize = feedCandidates.size();

            // 캐시 저장
            if (userId != null && !scoredDiaries.isEmpty()) {
                List<Long> diaryIds = scoredDiaries.stream().map(Diary::getId).collect(Collectors.toList());
                recommendationCacheService.cacheFeed(userId, diaryIds);
            }
        }

        Map<Long, Long> likeCountMap = getLikeCountsForDiaries(pagedDiaries);
        Set<Long> likedDiaryIds = getLikedDiaryIds(pagedDiaries, userId);

        List<ResponseDTO> responseList = pagedDiaries.stream()
                .map(diary -> buildResponseDTOForFeed(diary, requestMetaInfo, userId, likeCountMap, likedDiaryIds))
                .collect(Collectors.toList());

        return new PageImpl<>(responseList, pageable, totalSize);
    }

    @Transactional
    public void logClick(String userId, Long diaryId) {
        if (feedClickRepository.existsByUserIdAndDiaryId(userId, diaryId)) {
            return;
        }
        feedClickRepository.save(new FeedClick(userId, diaryId));

        // 클릭한 일기의 토픽으로 사용자 선호도 업데이트
        updateUserPreferenceOnClick(userId, diaryId);
    }

    private void updateUserPreferenceOnClick(String userId, Long diaryId) {
        try {
            // DiaryMetadata에서 토픽 조회
            String topic = diaryMetadataService.getMetadata(diaryId)
                    .map(meta -> meta.getPrimaryTopic())
                    .orElse("daily");

            userPreferenceService.recordInteraction(userId, topic, "CLICK");
            log.debug("클릭 기반 선호도 업데이트 - userId: {}, topic: {}", userId, topic);
        } catch (Exception e) {
            log.warn("선호도 업데이트 실패 - userId: {}, diaryId: {}", userId, diaryId);
        }
    }

    private List<Diary> applyRecommendationScoring(List<Diary> candidates, String userId,
            RequestMetaInfo requestMetaInfo) {
        if (userId == null || candidates.isEmpty()) {
            return candidates;
        }

        List<String> friendIds = friendRequestService.findFriendIdList(Pageable.unpaged(), userId, requestMetaInfo);
        Set<String> friendIdSet = new HashSet<>(friendIds);

        // 친구 일기와 공개 일기 분리
        List<Long> friendDiaryIds = candidates.stream()
                .filter(d -> friendIdSet.contains(d.getUser().getId()))
                .map(Diary::getId)
                .collect(Collectors.toList());

        List<Long> publicDiaryIds = candidates.stream()
                .filter(d -> !friendIdSet.contains(d.getUser().getId()))
                .map(Diary::getId)
                .collect(Collectors.toList());

        // 추천 서비스로 정렬
        List<Long> scoredIds = feedCompositionService.composeFeed(
                userId, friendDiaryIds, publicDiaryIds, candidates.size());

        // ID 순서대로 Diary 재정렬
        Map<Long, Diary> diaryMap = candidates.stream()
                .collect(Collectors.toMap(Diary::getId, d -> d));

        return scoredIds.stream()
                .map(diaryMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<Diary> collectFeedCandidates(Pageable pageable, String userId, RequestMetaInfo requestMetaInfo) {
        List<Long> clickedFeedIds = Collections.emptyList();
        List<Diary> unreadFriendFeeds = Collections.emptyList();

        if (userId != null) {
            List<String> friendIds = friendRequestService.findFriendIdList(pageable, userId, requestMetaInfo);
            clickedFeedIds = feedClickRepository.findClickedDiaryIdsByUserId(userId);

            unreadFriendFeeds = diaryRepository.findUnreadFeedsByVisibilityAndUserIds(
                    Status.FRIENDS, friendIds, clickedFeedIds);
        }

        List<Diary> unreadPublicFeeds = diaryRepository.findUnreadPublicFeeds(clickedFeedIds);

        List<Diary> combined = new ArrayList<>();
        combined.addAll(unreadFriendFeeds);
        combined.addAll(unreadPublicFeeds);

        // 미읽음 피드가 부족하면 읽은 피드도 포함 (폴백)
        int minFeedCount = pageable.getPageSize() * 2;
        if (combined.size() < minFeedCount) {
            log.info("미읽음 피드 부족 ({}/{}), 읽은 피드 포함", combined.size(), minFeedCount);
            List<Diary> allPublicFeeds = diaryRepository.findByStatusOrderByCreatedAtDesc(Status.PUBLIC);

            Set<Long> existingIds = combined.stream().map(Diary::getId).collect(Collectors.toSet());
            List<Diary> additionalFeeds = allPublicFeeds.stream()
                    .filter(d -> !existingIds.contains(d.getId()))
                    .limit(minFeedCount - combined.size())
                    .collect(Collectors.toList());

            combined.addAll(additionalFeeds);
        }

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
