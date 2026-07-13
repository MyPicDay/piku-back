package com.pikume.back.feed.application.service;

import com.pikume.back.diary.adapter.out.persistence.DiaryJpaRepository;
import com.pikume.back.diary.adapter.out.persistence.PhotoJpaRepository;
import com.pikume.back.diary.domain.Diary;
import com.pikume.back.diary.domain.Photo;
import com.pikume.back.diary.domain.vo.DiaryVisibility;
import com.pikume.back.feed.adapter.out.persistence.FeedClickJpaRepository;
import com.pikume.back.feed.application.dto.FeedCursorPage;
import com.pikume.back.feed.application.dto.FeedCursorRequest;
import com.pikume.back.feed.application.dto.FeedDiaryResult;
import com.pikume.back.feed.application.dto.FeedSortMode;
import com.pikume.back.feed.application.port.out.LoadRecommendationForFeedPort;
import com.pikume.back.feed.domain.FeedClick;
import com.pikume.back.social.adapter.out.persistence.CommentJpaRepository;
import com.pikume.back.social.adapter.out.persistence.FriendJpaRepository;
import com.pikume.back.social.adapter.out.persistence.FriendRequestJpaRepository;
import com.pikume.back.social.adapter.out.persistence.LikeJpaRepository;
import com.pikume.back.social.domain.comment.Comment;
import com.pikume.back.social.domain.friend.Friend;
import com.pikume.back.social.domain.like.Like;
import com.pikume.back.testsupport.AbstractJpaQueryCountIntegrationTest;
import com.pikume.back.user.adapter.out.persistence.UserJpaRepository;
import com.pikume.back.user.domain.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

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

	@Autowired
	private FeedClickJpaRepository feedClickJpaRepository;

	@Autowired
	private EntityManager entityManager;

	@MockitoBean
	private LoadRecommendationForFeedPort loadRecommendationForFeedPort;

	private User viewer;
	private Diary ownDiary;
	private Diary privateDiary;
	private Diary friendHigh;
	private Diary friendMid;
	private Diary friendLow;
	private Diary friendExtra;
	private Diary friendConsumed;
	private Diary publicHigh;
	private Diary publicLow;
	private Diary nonFriendFriendsDiary;

    @BeforeEach
	void setUp() {
		LocalDateTime baseTime = LocalDateTime.of(2026, 3, 8, 12, 0);
		LocalDate baseDate = LocalDate.of(2026, 3, 8);

		viewer = saveUser("viewer");
		User friendA = saveUser("friend-a");
		User friendB = saveUser("friend-b");
		User friendC = saveUser("friend-c");
		User friendD = saveUser("friend-d");
		User friendE = saveUser("friend-e");
		User publicAuthor1 = saveUser("public-1");
		User publicAuthor2 = saveUser("public-2");
		User nonFriend = saveUser("non-friend");
		User privateAuthor = saveUser("private");
		User liker1 = saveUser("liker-1");
		User liker2 = saveUser("liker-2");
		User commenter = saveUser("commenter");

		friendJpaRepository.save(new Friend(viewer.getId(), friendA.getId()));
		friendJpaRepository.save(new Friend(viewer.getId(), friendB.getId()));
		friendJpaRepository.save(new Friend(viewer.getId(), friendC.getId()));
		friendJpaRepository.save(new Friend(viewer.getId(), friendD.getId()));
		friendJpaRepository.save(new Friend(viewer.getId(), friendE.getId()));

		ownDiary = saveDiary(viewer.getId(), "my-feed", DiaryVisibility.PUBLIC, baseDate, baseTime.minusMinutes(1));
		privateDiary = saveDiary(privateAuthor.getId(), "private-feed", DiaryVisibility.PRIVATE, baseDate, baseTime.minusMinutes(2));
		friendHigh = saveDiary(friendA.getId(), "friend-high", DiaryVisibility.PUBLIC, baseDate.minusDays(6), baseTime.minusHours(4));
		friendMid = saveDiary(friendB.getId(), "friend-mid", DiaryVisibility.FRIENDS, baseDate.minusDays(3), baseTime.minusHours(3));
		friendLow = saveDiary(friendC.getId(), "friend-low", DiaryVisibility.PUBLIC, baseDate.minusDays(2), baseTime.minusHours(2));
		friendExtra = saveDiary(friendD.getId(), "friend-extra", DiaryVisibility.FRIENDS, baseDate.minusDays(5), baseTime.minusHours(1));
		friendConsumed = saveDiary(friendE.getId(), "friend-consumed", DiaryVisibility.PUBLIC, baseDate.minusDays(7), baseTime.minusHours(5));
		publicHigh = saveDiary(publicAuthor1.getId(), "public-high", DiaryVisibility.PUBLIC, baseDate.minusDays(1), baseTime.minusMinutes(20));
		publicLow = saveDiary(publicAuthor2.getId(), "public-low", DiaryVisibility.PUBLIC, baseDate.minusDays(4), baseTime.minusMinutes(10));
		nonFriendFriendsDiary = saveDiary(nonFriend.getId(), "non-friend-friends", DiaryVisibility.FRIENDS, baseDate.plusDays(1), baseTime.minusMinutes(5));

		saveRepresentPhoto(ownDiary, "my-feed.jpg");
		saveRepresentPhoto(privateDiary, "private-feed.jpg");
		saveRepresentPhoto(friendHigh, "friend-high.jpg");
		saveRepresentPhoto(friendMid, "friend-mid.jpg");
		saveRepresentPhoto(friendLow, "friend-low.jpg");
		saveRepresentPhoto(friendExtra, "friend-extra.jpg");
		saveRepresentPhoto(friendConsumed, "friend-consumed.jpg");
		saveRepresentPhoto(publicHigh, "public-high.jpg");
		saveRepresentPhoto(publicLow, "public-low.jpg");
		saveRepresentPhoto(nonFriendFriendsDiary, "non-friend-friends.jpg");

		addLike(friendHigh.getId(), liker1.getId());
		addLike(friendHigh.getId(), liker2.getId());
		addLike(friendHigh.getId(), commenter.getId());
		addComment(friendHigh.getId(), commenter.getId(), "fh-comment-1");
		addComment(friendHigh.getId(), liker1.getId(), "fh-comment-2");

		addLike(friendMid.getId(), liker1.getId());
		addLike(friendMid.getId(), liker2.getId());
		addComment(friendMid.getId(), commenter.getId(), "fm-comment-1");

		addLike(friendLow.getId(), liker1.getId());
		addLike(friendConsumed.getId(), liker1.getId());
		addLike(friendConsumed.getId(), liker2.getId());
		addComment(friendConsumed.getId(), commenter.getId(), "fc-comment-1");

		addLike(publicHigh.getId(), liker1.getId());
		addLike(publicHigh.getId(), liker2.getId());
		addLike(publicHigh.getId(), commenter.getId());
		addComment(publicHigh.getId(), commenter.getId(), "ph-comment-1");

		feedClickJpaRepository.save(new FeedClick(viewer.getId(), friendConsumed.getId()));
		flushAndClear();
	}

	@Test
	@DisplayName("피드 cursor 조회는 row 수가 커져도 쿼리 수가 일정하게 유지된다")
	void feedCursorQueryCountStaysBounded() {
		long oneItemQueries = measurePreparedStatements(() ->
				feedQueryService.getAllDiaries(new FeedCursorRequest(null, 1), viewer.getId()));
		long threeItemQueries = measurePreparedStatements(() ->
				feedQueryService.getAllDiaries(new FeedCursorRequest(null, 3), viewer.getId()));

		assertThat(threeItemQueries)
				.as("cursor limit이 커져도 materialize 단계의 사진/유저/좋아요/댓글/친구 상태 조회는 배치로 제한해야 한다")
				.isEqualTo(oneItemQueries);
	}

	@Test
	@DisplayName("첫 페이지는 not consumed friend bucket을 최신성 우선으로 반환하며 self/private는 제외한다")
	void firstPagePrioritizesNotConsumedFriendBucket() {
		FeedCursorPage<FeedDiaryResult> page = feedQueryService.getAllDiaries(
				new FeedCursorRequest(null, 4),
				viewer.getId());

		assertThat(page.items()).extracting(FeedDiaryResult::getDiaryId)
				.containsExactly(friendExtra.getId(), friendLow.getId(), friendMid.getId(), friendHigh.getId());
		assertThat(page.items()).extracting(FeedDiaryResult::getDiaryId)
				.doesNotContain(
						ownDiary.getId(),
						privateDiary.getId(),
						nonFriendFriendsDiary.getId(),
						friendConsumed.getId(),
						publicHigh.getId());
		assertThat(page.nextCursor()).isNotBlank();
		assertThat(page.hasNext()).isTrue();
	}

	@Test
	@DisplayName("nextCursor로 다음 페이지를 요청하면 다음 bucket으로 이어진다")
	void nextCursorContinuesIntoNextBucket() {
		FeedCursorPage<FeedDiaryResult> firstPage = feedQueryService.getAllDiaries(
				new FeedCursorRequest(null, 4),
				viewer.getId());

		FeedCursorPage<FeedDiaryResult> secondPage = feedQueryService.getAllDiaries(
				new FeedCursorRequest(firstPage.nextCursor(), 3),
				viewer.getId());

		assertThat(secondPage.items()).extracting(FeedDiaryResult::getDiaryId)
				.containsExactly(publicLow.getId(), publicHigh.getId(), friendConsumed.getId());
		assertThat(secondPage.items()).extracting(FeedDiaryResult::getDiaryId)
				.doesNotContain(friendHigh.getId(), friendMid.getId(), friendLow.getId(), friendExtra.getId());
	}

	@Test
	@DisplayName("최신순 첫 페이지는 공개 일기와 조회 가능한 친구공개 일기를 date DESC, diaryId DESC 순서로 반환한다")
	void latestFirstPageReturnsVisibleDiariesByGlobalDateOrder() {
		FeedCursorPage<FeedDiaryResult> page = feedQueryService.getAllDiaries(
				new FeedCursorRequest(null, 7, FeedSortMode.LATEST),
				viewer.getId());

		assertThat(page.items()).extracting(FeedDiaryResult::getDiaryId)
				.containsExactly(
						publicHigh.getId(),
						friendLow.getId(),
						friendMid.getId(),
						publicLow.getId(),
						friendExtra.getId(),
						friendHigh.getId(),
						friendConsumed.getId());
		assertThat(page.items()).extracting(FeedDiaryResult::getDiaryId)
				.doesNotContain(ownDiary.getId(), privateDiary.getId(), nonFriendFriendsDiary.getId());
	}

	@Test
	@DisplayName("최신순 nextCursor는 중복 없이 다음 date 구간으로 이어진다")
	void latestNextCursorContinuesWithoutDuplicates() {
		FeedCursorPage<FeedDiaryResult> firstPage = feedQueryService.getAllDiaries(
				new FeedCursorRequest(null, 3, FeedSortMode.LATEST),
				viewer.getId());

		FeedCursorPage<FeedDiaryResult> secondPage = feedQueryService.getAllDiaries(
				new FeedCursorRequest(firstPage.nextCursor(), 3, FeedSortMode.LATEST),
				viewer.getId());

		assertThat(firstPage.items()).extracting(FeedDiaryResult::getDiaryId)
				.containsExactly(publicHigh.getId(), friendLow.getId(), friendMid.getId());
		assertThat(secondPage.items()).extracting(FeedDiaryResult::getDiaryId)
				.containsExactly(publicLow.getId(), friendExtra.getId(), friendHigh.getId());
		assertThat(secondPage.items()).extracting(FeedDiaryResult::getDiaryId)
				.doesNotContain(publicHigh.getId(), friendLow.getId(), friendMid.getId());
	}

	@Test
	@DisplayName("최신순은 date가 같으면 diaryId DESC로 순서를 고정한다")
	void latestSortBreaksDateTiesByDiaryIdDescending() {
		User publicTieA = saveUser("public-tie-a");
		User publicTieB = saveUser("public-tie-b");
		LocalDate tiedDate = LocalDate.of(2026, 3, 9);
		Diary olderId = saveDiary(publicTieA.getId(), "public-tie-a", DiaryVisibility.PUBLIC,
				tiedDate, LocalDateTime.of(2026, 3, 8, 11, 55));
		Diary newerId = saveDiary(publicTieB.getId(), "public-tie-b", DiaryVisibility.PUBLIC,
				tiedDate, LocalDateTime.of(2026, 3, 8, 11, 50));
		saveRepresentPhoto(olderId, "public-tie-a.jpg");
		saveRepresentPhoto(newerId, "public-tie-b.jpg");
		flushAndClear();

		FeedCursorPage<FeedDiaryResult> page = feedQueryService.getAllDiaries(
				new FeedCursorRequest(null, 4, FeedSortMode.LATEST),
				viewer.getId());

		assertThat(page.items()).extracting(FeedDiaryResult::getDiaryId)
				.containsExactly(newerId.getId(), olderId.getId(), publicHigh.getId(), friendLow.getId());
	}

	@Test
	@DisplayName("비로그인 최신순은 PUBLIC 일기만 반환한다")
	void anonymousLatestFeedReturnsPublicDiariesOnly() {
		FeedCursorPage<FeedDiaryResult> page = feedQueryService.getAllDiaries(
				new FeedCursorRequest(null, 5, FeedSortMode.LATEST),
				null);

		assertThat(page.items()).extracting(FeedDiaryResult::getDiaryId)
				.containsExactly(
						ownDiary.getId(),
						publicHigh.getId(),
						friendLow.getId(),
						publicLow.getId(),
						friendHigh.getId());
		assertThat(page.items()).extracting(FeedDiaryResult::getDiaryId)
				.doesNotContain(privateDiary.getId(), friendMid.getId(), friendExtra.getId(), nonFriendFriendsDiary.getId());
	}

	@Test
	@DisplayName("피드 상세와 목록의 댓글 수는 삭제 댓글을 제외한다")
	void feedCommentCountExcludesDeletedComments() {
		User deletedCommenter = saveUser("deleted-commenter");
		Comment deletedComment = addComment(publicHigh.getId(), deletedCommenter.getId(), "deleted-comment");
		deletedComment.delete();
		commentJpaRepository.save(deletedComment);
		flushAndClear();

		FeedDiaryResult detail = feedQueryService.getDiaryWithPhotos(publicHigh.getId(), viewer.getId());
		FeedCursorPage<FeedDiaryResult> latestPage = feedQueryService.getAllDiaries(
				new FeedCursorRequest(null, 7, FeedSortMode.LATEST),
				viewer.getId());

		FeedDiaryResult publicHighItem = latestPage.items().stream()
				.filter(item -> item.getDiaryId().equals(publicHigh.getId()))
				.findFirst()
				.orElseThrow();
		assertThat(detail.getCommentCount()).isEqualTo(1L);
		assertThat(publicHighItem.getCommentCount()).isEqualTo(1L);
	}

	private User saveUser(String suffix) {
		String nicknameSuffix = suffix.substring(0, Math.min(suffix.length(), 15));
		return userJpaRepository.save(new User(
				suffix + "@example.com",
				"encoded-password",
				"nick-" + nicknameSuffix,
				"avatars/" + suffix + ".png"));
	}

	private Diary saveDiary(String userId, String content, DiaryVisibility visibility, LocalDateTime createdAt) {
		return saveDiary(userId, content, visibility, LocalDate.now(), createdAt);
	}

	private Diary saveDiary(String userId, String content, DiaryVisibility visibility, LocalDate date,
			LocalDateTime createdAt) {
		Diary diary = diaryJpaRepository.save(new Diary(content, visibility, date, userId));
		entityManager.createNativeQuery("UPDATE diary SET created_at = :createdAt WHERE id = :diaryId")
				.setParameter("createdAt", createdAt)
				.setParameter("diaryId", diary.getId())
				.executeUpdate();
		return diary;
	}

	private void saveRepresentPhoto(Diary diary, String fileName) {
        String PUBLIC_PREFIX = "public/";
        Photo photo = new Photo(diary, PUBLIC_PREFIX + diary.getUserId() + "/" + fileName, 0);
		photo.updateRepresent(true);
		photoJpaRepository.save(photo);
	}

	private void addLike(Long diaryId, String userId) {
		likeJpaRepository.save(Like.builder().userId(userId).diaryId(diaryId).build());
	}

	private Comment addComment(Long diaryId, String userId, String content) {
		return commentJpaRepository.save(new Comment(content, userId, diaryId));
	}
}
