package store.piku.back.like.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.piku.back.diary.entity.Diary;
import store.piku.back.diary.repository.DiaryRepository;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.like.dto.LikeResponse;
import store.piku.back.like.entity.Like;
import store.piku.back.like.exception.LikeErrorCode;
import store.piku.back.like.exception.LikeException;
import store.piku.back.like.repository.LikeRepository;
import store.piku.back.notification.entity.NotificationType;
import store.piku.back.notification.service.NotificationService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LikeService {

    private final LikeRepository likeRepository;
    private final DiaryRepository diaryRepository;
    private final NotificationService notificationService;

    @Transactional
    public LikeResponse addLike(String userId, Long diaryId, RequestMetaInfo requestMetaInfo) {
        log.info("[좋아요 추가 요청] userId: {}, diaryId: {}", userId, diaryId);

        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new LikeException(LikeErrorCode.DIARY_NOT_FOUND));

        if (diary.getUser().getId().equals(userId)) {
            throw new LikeException(LikeErrorCode.CANNOT_LIKE_OWN_DIARY);
        }

        Optional<Like> existingLike = likeRepository.findByUserIdAndDiaryId(userId, diaryId);
        if (existingLike.isPresent()) {
            throw new LikeException(LikeErrorCode.ALREADY_LIKED);
        }

        Like like = Like.builder()
                .userId(userId)
                .diaryId(diaryId)
                .build();
        likeRepository.save(like);

        String diaryOwnerId = diary.getUser().getId();
        notificationService.sendNotification(
                diaryOwnerId,
                NotificationType.LIKE,
                userId,
                diary,
                requestMetaInfo
        );

        long likeCount = likeRepository.countByDiaryId(diaryId);
        log.info("[좋아요 추가 완료] diaryId: {}, 총 좋아요 수: {}", diaryId, likeCount);

        return LikeResponse.builder()
                .diaryId(diaryId)
                .likeCount(likeCount)
                .isLiked(true)
                .build();
    }

    @Transactional
    public LikeResponse removeLike(String userId, Long diaryId) {
        log.info("[좋아요 취소 요청] userId: {}, diaryId: {}", userId, diaryId);

        if (!diaryRepository.existsById(diaryId)) {
            throw new LikeException(LikeErrorCode.DIARY_NOT_FOUND);
        }

        Like like = likeRepository.findByUserIdAndDiaryId(userId, diaryId)
                .orElseThrow(() -> new LikeException(LikeErrorCode.LIKE_NOT_FOUND));

        like.inactive();

        long likeCount = likeRepository.countByDiaryId(diaryId);
        log.info("[좋아요 취소 완료] diaryId: {}, 총 좋아요 수: {}", diaryId, likeCount);

        return LikeResponse.builder()
                .diaryId(diaryId)
                .likeCount(likeCount)
                .isLiked(false)
                .build();
    }

    @Transactional(readOnly = true)
    public LikeResponse getLikeStatus(String userId, Long diaryId) {
        if (!diaryRepository.existsById(diaryId)) {
            throw new LikeException(LikeErrorCode.DIARY_NOT_FOUND);
        }

        long likeCount = likeRepository.countByDiaryId(diaryId);
        boolean isLiked = userId != null && likeRepository.existsByUserIdAndDiaryId(userId, diaryId);

        return LikeResponse.builder()
                .diaryId(diaryId)
                .likeCount(likeCount)
                .isLiked(isLiked)
                .build();
    }

    @Transactional(readOnly = true)
    public long getLikeCount(Long diaryId) {
        return likeRepository.countByDiaryId(diaryId);
    }

    @Transactional(readOnly = true)
    public boolean isLikedByUser(String userId, Long diaryId) {
        if (userId == null) return false;
        return likeRepository.existsByUserIdAndDiaryId(userId, diaryId);
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> getLikeCountsForDiaries(List<Long> diaryIds) {
        if (diaryIds == null || diaryIds.isEmpty()) {
            return Map.of();
        }

        List<Object[]> results = likeRepository.countByDiaryIds(diaryIds);
        return results.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }

    @Transactional(readOnly = true)
    public Set<Long> getLikedDiaryIds(String userId, List<Long> diaryIds) {
        if (userId == null || diaryIds == null || diaryIds.isEmpty()) {
            return Set.of();
        }
        return likeRepository.findLikedDiaryIdsByUserIdAndDiaryIds(userId, diaryIds);
    }
}
