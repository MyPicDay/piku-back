package com.pikume.back.user.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.pikume.back.user.application.dto.ProfilePreviewResult;
import com.pikume.back.user.application.dto.UserProfileResult;
import com.pikume.back.user.application.exception.UserErrorCode;
import com.pikume.back.user.application.exception.UserNotFoundException;
import com.pikume.back.user.application.port.out.LoadUserForProfilePort;
import com.pikume.back.user.application.port.out.QueryProfileDiaryMetricsPort;
import com.pikume.back.user.application.port.out.QueryProfileSocialMetricsPort;
import com.pikume.back.user.domain.User;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileQueryService")
class UserProfileQueryServiceTest {

	@InjectMocks
	private UserProfileQueryService service;

	@Mock
	private LoadUserForProfilePort loadUserForProfilePort;
	@Mock
	private QueryProfileSocialMetricsPort socialMetricsPort;
	@Mock
	private QueryProfileDiaryMetricsPort diaryMetricsPort;

	@Test
	@DisplayName("프로필 미리보기를 정상 조회한다")
	void queryProfilePreview() {
		User user = new User("user-1", "test@test.com", "pw", "피쿠", "avatar.jpg");
		given(loadUserForProfilePort.loadProfileUser("user-1")).willReturn(Optional.of(user));
		given(socialMetricsPort.queryFriendCount("user-1")).willReturn(5);
		given(diaryMetricsPort.queryVisibleDiaryCount("user-1", "viewer-1")).willReturn(10L);
		given(socialMetricsPort.queryFriendshipStatus("viewer-1", "user-1")).willReturn("NONE");

		ProfilePreviewResult result = service.queryProfilePreview("user-1", "viewer-1");

		assertThat(result.id()).isEqualTo("user-1");
		assertThat(result.nickname()).isEqualTo("피쿠");
		assertThat(result.avatarObjectKey()).isEqualTo("avatar.jpg");
		assertThat(result.friendCount()).isEqualTo(5);
		assertThat(result.diaryCount()).isEqualTo(10L);
		assertThat(result.friendStatus()).isEqualTo("NONE");
	}

	@Test
	@DisplayName("존재하지 않는 사용자 프로필 조회 시 예외 발생")
	void profileNotFound() {
		given(loadUserForProfilePort.loadProfileUser("unknown")).willReturn(Optional.empty());

			assertThatThrownBy(() -> service.queryProfilePreview("unknown", "viewer"))
					.isInstanceOfSatisfying(UserNotFoundException.class,
							ex -> assertThat(ex.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND));
	}

	@Test
	@DisplayName("프로필 상세 조회 시 소유자 여부와 월별 일기를 포함한다")
	void queryUserProfileWithOwnership() {
		User user = new User("user-1", "test@test.com", "pw", "피쿠", "avatar.jpg");
		given(loadUserForProfilePort.loadProfileUser("user-1")).willReturn(Optional.of(user));
		given(socialMetricsPort.queryFriendCount("user-1")).willReturn(3);
		given(diaryMetricsPort.queryVisibleDiaryCount("user-1", "user-1")).willReturn(7L);
		given(socialMetricsPort.queryFriendshipStatus("user-1", "user-1")).willReturn("NONE");
		given(diaryMetricsPort.queryVisibleMonthlyDiaryCounts("user-1", "user-1")).willReturn(
				List.of(new QueryProfileDiaryMetricsPort.MonthlyDiaryCount(2026, 1, 5),
						new QueryProfileDiaryMetricsPort.MonthlyDiaryCount(2026, 2, 2)));

		UserProfileResult result = service.queryUserProfile("user-1", "user-1");

		assertThat(result.isOwner()).isTrue();
		assertThat(result.monthlyDiaryCount()).hasSize(2);
	}
}
