package com.pikume.back.feed.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.pikume.back.diary.adapter.in.web.dto.ResponseDTO;
import com.pikume.back.diary.adapter.out.persistence.DiaryJpaRepository;
import com.pikume.back.diary.adapter.out.persistence.PhotoJpaRepository;
import com.pikume.back.diary.adapter.out.storage.PhotoConstants;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.Photo;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import com.pikume.back.feed.application.port.out.LoadRecommendationForFeedPort;
import com.pikume.back.social.adapter.out.persistence.FriendJpaRepository;
import com.pikume.back.social.adapter.out.persistence.FriendRequestJpaRepository;
import com.pikume.back.social.adapter.out.persistence.LikeJpaRepository;
import com.pikume.back.social.adapter.out.persistence.CommentJpaRepository;
import com.pikume.back.social.domain.comment.Comment;
import com.pikume.back.social.domain.friend.Friend;
import com.pikume.back.social.domain.friend.FriendRequest;
import com.pikume.back.social.domain.friend.vo.FriendStatus;
import com.pikume.back.social.domain.like.Like;
import com.pikume.back.testsupport.AbstractJpaQueryCountIntegrationTest;
import com.pikume.back.user.adapter.out.persistence.UserJpaRepository;
import com.pikume.back.user.domain.User;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

class FeedQueryServiceQueryIntegrationTest extends AbstractJpaQueryCountIntegrationTest {

	@Autowired
	private FeedQueryService feedQueryService;

	@Autowired
	private UserJpaRepository userJpaRepository;

	@Autowired
	private DiaryJpaRepository diaryJpaRepository;

	@Autowired
	private PhotoJpaRepository photoJpaRepository;

	@Autowired
	private FriendJpaRepository friendJpaRepository;

	@Autowired
	private FriendRequestJpaRepository friendRequestJpaRepository;

	@Autowired
	private LikeJpaRepository likeJpaRepository;

	@Autowired
	private CommentJpaRepository commentJpaRepository;

	@MockitoBean
	private LoadRecommendationForFeedPort loadRecommendationForFeedPort;

	private User viewer;
	private Diary ownDiary;
	private Diary privateDiary;
	private Diary diary1;
	private Diary diary2;
	private Diary diary3;

	@BeforeEach
	void setUp() {
		viewer = saveUser("viewer");
		User friendAuthor = saveUser("friend-author");
		User requestedAuthor = saveUser("requested-author");
		User receivedAuthor = saveUser("received-author");
		User liker = saveUser("liker");
		User privateAuthor = saveUser("private-author");

		ownDiary = saveDiary(viewer.getId(), "my-feed");
		privateDiary = saveDiary(privateAuthor.getId(), "private-feed", DiaryVisibility.PRIVATE);
		diary1 = saveDiary(friendAuthor.getId(), "feed-1");
		diary2 = saveDiary(requestedAuthor.getId(), "feed-2");
		diary3 = saveDiary(receivedAuthor.getId(), "feed-3");

		saveRepresentPhoto(ownDiary, "my-feed.jpg");
		saveRepresentPhoto(privateDiary, "private-feed.jpg");
		saveRepresentPhoto(diary1, "feed-1.jpg");
		saveRepresentPhoto(diary2, "feed-2.jpg");
		saveRepresentPhoto(diary3, "feed-3.jpg");

		friendJpaRepository.save(new Friend(viewer.getId(), friendAuthor.getId()));
		friendRequestJpaRepository.save(new FriendRequest(viewer.getId(), requestedAuthor.getId()));
		friendRequestJpaRepository.save(new FriendRequest(receivedAuthor.getId(), viewer.getId()));

		saveComment(diary1.getId(), friendAuthor.getId(), "comment-1");
		saveComment(diary2.getId(), requestedAuthor.getId(), "comment-2");
		saveComment(diary3.getId(), receivedAuthor.getId(), "comment-3");

		likeJpaRepository.save(Like.builder().userId(viewer.getId()).diaryId(diary2.getId()).build());
		likeJpaRepository.save(Like.builder().userId(liker.getId()).diaryId(diary1.getId()).build());
		likeJpaRepository.save(Like.builder().userId(liker.getId()).diaryId(diary3.getId()).build());

		given(loadRecommendationForFeedPort.getCachedFeed(viewer.getId()))
				.willReturn(List.of(diary3.getId(), privateDiary.getId(), ownDiary.getId(), diary1.getId(), diary2.getId()));
	}

	@Test
	@DisplayName("피드 목록 조회는 row 수가 커져도 쿼리 수가 일정하게 유지된다")
	void feedQueryCountStaysBounded() {
		long oneItemQueries = measurePreparedStatements(() ->
				feedQueryService.getAllDiaries(PageRequest.of(0, 1), REQUEST_META_INFO, viewer.getId()));
		long threeItemQueries = measurePreparedStatements(() ->
				feedQueryService.getAllDiaries(PageRequest.of(0, 3), REQUEST_META_INFO, viewer.getId()));

		assertThat(threeItemQueries)
				.as("피드 row 수가 늘어도 사진/유저/좋아요/댓글/친구 상태 조회는 배치로 제한해야 한다")
				.isEqualTo(oneItemQueries);
	}

	@Test
	@DisplayName("캐시된 피드 ID 순서를 유지한 채 목록을 materialize 한다")
	void feedOrderFollowsCachedIds() {
		Page<ResponseDTO> page = feedQueryService.getAllDiaries(PageRequest.of(0, 3), REQUEST_META_INFO, viewer.getId());

		assertThat(page.getContent()).extracting(ResponseDTO::getDiaryId)
				.containsExactly(diary3.getId(), diary1.getId(), diary2.getId());
		assertThat(page.getContent()).extracting(ResponseDTO::getFriendStatus)
				.containsExactly(
						FriendStatus.RECEIVED,
						FriendStatus.FRIENDS,
						FriendStatus.REQUESTED);
	}

	@Test
	@DisplayName("캐시 hit 경로에서도 본인 일기는 목록에서 제외된다")
	void feedExcludesOwnDiaryOnCacheHit() {
		Page<ResponseDTO> page = feedQueryService.getAllDiaries(PageRequest.of(0, 10), REQUEST_META_INFO, viewer.getId());

		assertThat(page.getContent()).extracting(ResponseDTO::getDiaryId)
				.doesNotContain(ownDiary.getId());
	}

	@Test
	@DisplayName("캐시 hit 경로에서도 PRIVATE 일기는 피드에서 제외된다")
	void feedExcludesPrivateDiaryOnCacheHit() {
		Page<ResponseDTO> page = feedQueryService.getAllDiaries(PageRequest.of(0, 10), REQUEST_META_INFO, viewer.getId());

		assertThat(page.getContent()).extracting(ResponseDTO::getDiaryId)
				.doesNotContain(privateDiary.getId());
	}

	private User saveUser(String suffix) {
		return userJpaRepository.save(new User(
				suffix + "@example.com",
				"encoded-password",
				"nick-" + suffix,
				"avatars/" + suffix + ".png"));
	}

	private Diary saveDiary(String userId, String content) {
		return diaryJpaRepository.save(new Diary(content, DiaryVisibility.PUBLIC, LocalDate.now(), userId));
	}

	private Diary saveDiary(String userId, String content, DiaryVisibility visibility) {
		return diaryJpaRepository.save(new Diary(content, visibility, LocalDate.now(), userId));
	}

	private void saveRepresentPhoto(Diary diary, String fileName) {
		Photo photo = new Photo(diary, PhotoConstants.PUBLIC_PREFIX + diary.getUserId() + "/" + fileName, 0);
		photo.updateRepresent(true);
		photoJpaRepository.save(photo);
	}

	private void saveComment(Long diaryId, String userId, String content) {
		commentJpaRepository.save(new Comment(content, userId, diaryId));
	}
}
