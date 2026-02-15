package store.piku.back.social.application.port.in;

import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.social.adapter.in.web.dto.LikeResponse;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface LikeUseCase {

	LikeResponse addLike(String userId, Long diaryId, RequestMetaInfo requestMetaInfo);

	LikeResponse removeLike(String userId, Long diaryId);

	LikeResponse getLikeStatus(String userId, Long diaryId);

	long getLikeCount(Long diaryId);

	boolean isLikedByUser(String userId, Long diaryId);

	Map<Long, Long> getLikeCountsForDiaries(List<Long> diaryIds);

	Set<Long> getLikedDiaryIds(String userId, List<Long> diaryIds);
}
