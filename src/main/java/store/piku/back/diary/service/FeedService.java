package store.piku.back.diary.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.piku.back.social.application.port.in.CommentUseCase;
import store.piku.back.diary.adapter.in.web.dto.ResponseDTO;
import store.piku.back.diary.domain.Diary;
import store.piku.back.diary.entity.FeedClick;
import store.piku.back.diary.domain.Photo;
import store.piku.back.social.domain.friend.vo.FriendStatus;
import store.piku.back.diary.domain.vo.DiaryVisibility;
import store.piku.back.diary.application.port.out.LoadUserForDiaryPort;
import store.piku.back.diary.application.service.DiaryQueryService;
import store.piku.back.diary.adapter.out.persistence.DiaryJpaRepository;
import store.piku.back.diary.repository.FeedClickRepository;
import store.piku.back.diary.adapter.out.persistence.PhotoJpaRepository;
import store.piku.back.social.application.port.in.FriendUseCase;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.global.util.ImagePathToUrlConverter;
import store.piku.back.social.application.port.in.LikeUseCase;
import store.piku.back.recommendation._legacy.FeedCandidateCollector;
import store.piku.back.recommendation._legacy.FeedCompositionService;
import store.piku.back.recommendation.application.port.in.AnalyzeDiaryContentUseCase;
import store.piku.back.recommendation.application.port.in.CacheFeedUseCase;
import store.piku.back.recommendation.application.port.in.ManageUserPreferenceUseCase;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 피드 조회 서비스
 * 
 * 책임:
 * - 피드 조회 API 처리
 * - 캐시 관리 (조회/저장)
 * - 클릭 로깅
 * - ResponseDTO 변환
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FeedService {

    private final DiaryQueryService diaryQueryService;
    private final CommentUseCase commentUseCase;
    private final PhotoJpaRepository photoJpaRepository;
    private final DiaryJpaRepository diaryJpaRepository;
    private final ImagePathToUrlConverter imagePathToUrlConverter;
    private final FriendUseCase friendUseCase;
    private final FeedClickRepository feedClickRepository;
    private final LikeUseCase likeUseCase;
    private final LoadUserForDiaryPort loadUserForDiaryPort;

    // 추천 서비스
    private final FeedCandidateCollector feedCandidateCollector;
    private final FeedCompositionService feedCompositionService;
    private final CacheFeedUseCase cacheFeedUseCase;
    private final ManageUserPreferenceUseCase manageUserPreferenceUseCase;
    private final AnalyzeDiaryContentUseCase analyzeDiaryContentUseCase;

    @Transactional(readOnly = true)
    public ResponseDTO getDiaryWithPhotos(Long diaryId, RequestMetaInfo requestMetaInfo, String userId) {
        log.info("일기 상세 조회 요청 - diaryId: {}", diaryId);

        Diary diary = diaryQueryService.getDiaryById(diaryId);
        List<String> photoUrls = getPhotosForDiary(diary, requestMetaInfo);

        boolean isOwner = diary.getUserId().equals(userId);
        boolean isFriend = friendUseCase.areFriends(diary.getUserId(), userId);
        boolean hasAccess = isOwner || (diary.getStatus() == DiaryVisibility.PUBLIC)
                || (diary.getStatus() == DiaryVisibility.FRIENDS && isFriend);

        return buildResponseDTO(diary, photoUrls, requestMetaInfo, userId, null, hasAccess);
    }

    @Transactional(readOnly = true)
    public Page<ResponseDTO> getAllDiaries(Pageable pageable, RequestMetaInfo requestMetaInfo, String userId) {
        List<Diary> diaries = getRecommendedDiaries(userId, pageable, requestMetaInfo);
        List<Diary> pagedDiaries = applyPaging(diaries, pageable);

        List<ResponseDTO> responseList = convertToResponseDTOs(pagedDiaries, requestMetaInfo, userId);

        return new PageImpl<>(responseList, pageable, diaries.size());
    }

    @Transactional
    public void logClick(String userId, Long diaryId) {
        if (feedClickRepository.existsByUserIdAndDiaryId(userId, diaryId)) {
            return;
        }
        feedClickRepository.save(new FeedClick(userId, diaryId));
        updateUserPreferenceOnClick(userId, diaryId);

        // 클릭 시 캐시 무효화 → 다음 요청 시 새 순서 반영
        cacheFeedUseCase.invalidateCache(userId);
        log.debug("피드 캐시 무효화 - userId: {}", userId);
    }

    // ==================== Private Methods ====================

    private List<Diary> getRecommendedDiaries(String userId, Pageable pageable, RequestMetaInfo requestMetaInfo) {
        // 1. 캐시 확인
        List<Long> cachedIds = getCachedFeedIds(userId);
        if (!cachedIds.isEmpty()) {
            log.info("피드 캐시 히트 - userId: {}", userId);
            return getDiariesByIds(cachedIds, userId);
        }

        // 2. 캐시 미스: 후보 수집 → 스코어링 → 캐시 저장
        log.info("피드 캐시 미스 - userId: {}", userId);
        List<Diary> candidates = feedCandidateCollector.collect(userId, pageable, requestMetaInfo);
        List<Diary> scoredDiaries = applyScoring(candidates, userId, requestMetaInfo);
        cacheFeed(userId, scoredDiaries);

        return scoredDiaries;
    }

    private List<Long> getCachedFeedIds(String userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return cacheFeedUseCase.getCachedFeed(userId);
    }

    private List<Diary> getDiariesByIds(List<Long> diaryIds, String userId) {
        List<Diary> diaries = diaryJpaRepository.findAllById(diaryIds);
        return filterOwnDiaries(diaries, userId);
    }

    private List<Diary> filterOwnDiaries(List<Diary> diaries, String userId) {
        if (userId == null) {
            return diaries;
        }
        return diaries.stream()
                .filter(d -> !d.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    private List<Diary> applyScoring(List<Diary> candidates, String userId, RequestMetaInfo requestMetaInfo) {
        if (userId == null || candidates.isEmpty()) {
            return candidates;
        }

        List<String> friendIds = friendUseCase.findFriendIdList(Pageable.unpaged(), userId, requestMetaInfo);
        Set<String> friendIdSet = new HashSet<>(friendIds);

        List<Long> friendDiaryIds = candidates.stream()
                .filter(d -> friendIdSet.contains(d.getUserId()))
                .map(Diary::getId)
                .collect(Collectors.toList());

        List<Long> publicDiaryIds = candidates.stream()
                .filter(d -> !friendIdSet.contains(d.getUserId()))
                .map(Diary::getId)
                .collect(Collectors.toList());

        List<Long> scoredIds = feedCompositionService.composeFeed(
                userId, friendDiaryIds, publicDiaryIds, candidates.size());

        Map<Long, Diary> diaryMap = candidates.stream()
                .collect(Collectors.toMap(Diary::getId, d -> d));

        return scoredIds.stream()
                .map(diaryMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void cacheFeed(String userId, List<Diary> diaries) {
        if (userId == null || diaries.isEmpty()) {
            return;
        }
        List<Long> diaryIds = diaries.stream().map(Diary::getId).collect(Collectors.toList());
        cacheFeedUseCase.cacheFeed(userId, diaryIds);
    }

    private void updateUserPreferenceOnClick(String userId, Long diaryId) {
        try {
            String topic = analyzeDiaryContentUseCase.getMetadata(diaryId)
                    .map(meta -> meta.getPrimaryTopic())
                    .orElse("daily");
            manageUserPreferenceUseCase.recordInteraction(userId, topic, "CLICK");
            log.debug("클릭 기반 선호도 업데이트 - userId: {}, topic: {}", userId, topic);
        } catch (Exception e) {
            log.warn("선호도 업데이트 실패 - userId: {}, diaryId: {}", userId, diaryId);
        }
    }

    private List<Diary> applyPaging(List<Diary> diaries, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), diaries.size());
        return start >= diaries.size() ? Collections.emptyList() : diaries.subList(start, end);
    }

    private List<ResponseDTO> convertToResponseDTOs(List<Diary> diaries, RequestMetaInfo requestMetaInfo,
            String userId) {
        Map<Long, Long> likeCountMap = getLikeCountsForDiaries(diaries);
        Set<Long> likedDiaryIds = getLikedDiaryIds(diaries, userId);

        return diaries.stream()
                .map(diary -> buildResponseDTOForFeed(diary, requestMetaInfo, userId, likeCountMap, likedDiaryIds))
                .collect(Collectors.toList());
    }

    // ==================== DTO Builders ====================

    private List<String> getPhotosForDiary(Diary diary, RequestMetaInfo requestMetaInfo) {
        List<Photo> photos = photoJpaRepository.findByDiaryId(diary.getId());
        return diaryQueryService.sortPhotos(photos, requestMetaInfo);
    }

    private ResponseDTO buildResponseDTO(Diary diary, List<String> photoUrls, RequestMetaInfo requestMetaInfo,
            String userId, FriendStatus friendStatus, boolean hasFullAccess) {
        String avatar = loadUserForDiaryPort.getUserAvatar(diary.getUserId());
        String avatarUrl = imagePathToUrlConverter.userAvatarImageUrl(avatar, requestMetaInfo);
        long likeCount = likeUseCase.getLikeCount(diary.getId());
        boolean isLiked = likeUseCase.isLikedByUser(userId, diary.getId());

        List<String> displayPhotos = hasFullAccess ? photoUrls : List.of(photoUrls.get(0));
        String displayContent = hasFullAccess ? diary.getContent() : null;

        return ResponseDTO.builder()
                .diaryId(diary.getId())
                .status(diary.getStatus())
                .content(displayContent)
                .imgUrls(displayPhotos)
                .date(diary.getDate())
                .nickname(loadUserForDiaryPort.getUserNickname(diary.getUserId()))
                .avatar(avatarUrl)
                .userId(diary.getUserId())
                .createdAt(diary.getCreatedAt())
                .friendStatus(friendStatus)
                .commentCount(commentUseCase.countAllCommentsByDiaryId(diary.getId()))
                .likeCount(likeCount)
                .isLiked(isLiked)
                .build();
    }

    private ResponseDTO buildResponseDTOForFeed(Diary diary, RequestMetaInfo requestMetaInfo, String userId,
            Map<Long, Long> likeCountMap, Set<Long> likedDiaryIds) {
        List<String> photoUrls = getPhotosForDiary(diary, requestMetaInfo);
        String avatar = loadUserForDiaryPort.getUserAvatar(diary.getUserId());
        String avatarUrl = imagePathToUrlConverter.userAvatarImageUrl(avatar, requestMetaInfo);

        FriendStatus friendStatus = userId != null
                ? friendUseCase.getFriendshipStatus(userId, diary.getUserId())
                : FriendStatus.NONE;

        return ResponseDTO.builder()
                .diaryId(diary.getId())
                .status(diary.getStatus())
                .content(diary.getContent())
                .imgUrls(photoUrls)
                .date(diary.getDate())
                .nickname(loadUserForDiaryPort.getUserNickname(diary.getUserId()))
                .avatar(avatarUrl)
                .userId(diary.getUserId())
                .createdAt(diary.getCreatedAt())
                .friendStatus(friendStatus)
                .commentCount(commentUseCase.countAllCommentsByDiaryId(diary.getId()))
                .likeCount(likeCountMap.getOrDefault(diary.getId(), 0L))
                .isLiked(likedDiaryIds.contains(diary.getId()))
                .build();
    }

    private Map<Long, Long> getLikeCountsForDiaries(List<Diary> diaries) {
        List<Long> diaryIds = diaries.stream().map(Diary::getId).collect(Collectors.toList());
        return likeUseCase.getLikeCountsForDiaries(diaryIds);
    }

    private Set<Long> getLikedDiaryIds(List<Diary> diaries, String userId) {
        List<Long> diaryIds = diaries.stream().map(Diary::getId).collect(Collectors.toList());
        return likeUseCase.getLikedDiaryIds(userId, diaryIds);
    }
}
