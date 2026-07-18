package com.pikume.back.diary.application.policy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.pikume.back.diary.application.port.out.LoadFriendshipForDiaryPort;
import com.pikume.back.diary.domain.vo.DiaryVisibility;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiaryVisibilityPolicy")
class DiaryVisibilityPolicyTest {

	@InjectMocks
	private DiaryVisibilityPolicy diaryVisibilityPolicy;

	@Mock
	private LoadFriendshipForDiaryPort loadFriendshipForDiaryPort;

	@Test
	@DisplayName("작성자는 모든 공개 범위를 볼 수 있다")
	void ownerCanViewAll() {
		DiaryVisibility anonymous = DiaryVisibility.valueOf("ANONYMOUS");

		assertThat(diaryVisibilityPolicy.isHiddenFromViewer("owner-id", DiaryVisibility.PUBLIC, "owner-id")).isFalse();
		assertThat(diaryVisibilityPolicy.isHiddenFromViewer("owner-id", DiaryVisibility.FRIENDS, "owner-id")).isFalse();
		assertThat(diaryVisibilityPolicy.isHiddenFromViewer("owner-id", DiaryVisibility.PRIVATE, "owner-id")).isFalse();
		assertThat(diaryVisibilityPolicy.isHiddenFromViewer("owner-id", anonymous, "owner-id")).isFalse();
		assertThat(diaryVisibilityPolicy.visibleStatusesForOwner("owner-id", "owner-id"))
				.containsExactly(DiaryVisibility.PUBLIC, DiaryVisibility.FRIENDS, DiaryVisibility.PRIVATE, anonymous);
	}

	@Test
	@DisplayName("친구는 FRIENDS와 PUBLIC만 볼 수 있다")
	void friendCanViewFriendsAndPublic() {
		DiaryVisibility anonymous = DiaryVisibility.valueOf("ANONYMOUS");
		given(loadFriendshipForDiaryPort.areFriends("owner-id", "friend-id")).willReturn(true);

		assertThat(diaryVisibilityPolicy.isHiddenFromViewer("owner-id", DiaryVisibility.PUBLIC, "friend-id")).isFalse();
		assertThat(diaryVisibilityPolicy.isHiddenFromViewer("owner-id", DiaryVisibility.FRIENDS, "friend-id")).isFalse();
		assertThat(diaryVisibilityPolicy.isHiddenFromViewer("owner-id", DiaryVisibility.PRIVATE, "friend-id")).isTrue();
		assertThat(diaryVisibilityPolicy.isHiddenFromViewer("owner-id", anonymous, "friend-id")).isFalse();
		assertThat(diaryVisibilityPolicy.visibleStatusesForOwner("owner-id", "friend-id"))
				.containsExactly(DiaryVisibility.PUBLIC, DiaryVisibility.FRIENDS);
	}

	@Test
	@DisplayName("비친구는 PUBLIC만 볼 수 있다")
	void strangerCanOnlyViewPublic() {
		DiaryVisibility anonymous = DiaryVisibility.valueOf("ANONYMOUS");
		given(loadFriendshipForDiaryPort.areFriends("owner-id", "stranger-id")).willReturn(false);

		assertThat(diaryVisibilityPolicy.isHiddenFromViewer("owner-id", DiaryVisibility.PUBLIC, "stranger-id")).isFalse();
		assertThat(diaryVisibilityPolicy.isHiddenFromViewer("owner-id", DiaryVisibility.FRIENDS, "stranger-id")).isTrue();
		assertThat(diaryVisibilityPolicy.isHiddenFromViewer("owner-id", DiaryVisibility.PRIVATE, "stranger-id")).isTrue();
		assertThat(diaryVisibilityPolicy.isHiddenFromViewer("owner-id", anonymous, "stranger-id")).isFalse();
		assertThat(diaryVisibilityPolicy.visibleStatusesForOwner("owner-id", "stranger-id"))
				.isEqualTo(List.of(DiaryVisibility.PUBLIC));
	}

	@Test
	@DisplayName("비로그인 사용자는 PUBLIC만 볼 수 있다")
	void anonymousCanOnlyViewPublic() {
		DiaryVisibility anonymous = DiaryVisibility.valueOf("ANONYMOUS");

		assertThat(diaryVisibilityPolicy.isHiddenFromViewer("owner-id", DiaryVisibility.PUBLIC, null)).isFalse();
		assertThat(diaryVisibilityPolicy.isHiddenFromViewer("owner-id", DiaryVisibility.FRIENDS, null)).isTrue();
		assertThat(diaryVisibilityPolicy.isHiddenFromViewer("owner-id", DiaryVisibility.PRIVATE, null)).isTrue();
		assertThat(diaryVisibilityPolicy.isHiddenFromViewer("owner-id", anonymous, null)).isFalse();
		assertThat(diaryVisibilityPolicy.visibleStatusesForOwner("owner-id", null))
				.isEqualTo(List.of(DiaryVisibility.PUBLIC));
	}

	@Test
	@DisplayName("소유자 식별자가 누락돼도 비로그인 사용자를 작성자로 간주하지 않는다")
	void missingOwnerIdDoesNotMakeAnonymousViewerOwner() {
		assertThat(diaryVisibilityPolicy.isHiddenFromViewer(null, DiaryVisibility.PRIVATE, null)).isTrue();
		assertThat(diaryVisibilityPolicy.visibleStatusesForOwner(null, null))
				.containsExactly(DiaryVisibility.PUBLIC);
	}
}
