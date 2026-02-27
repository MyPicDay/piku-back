package com.pikume.back.user.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.pikume.back.global.dto.RequestMetaInfo;
import com.pikume.back.global.error.ErrorCode;
import com.pikume.back.global.exception.BusinessException;
import com.pikume.back.global.util.ImagePathToUrlConverter;
import com.pikume.back.user.application.dto.ProfilePreviewResult;
import com.pikume.back.user.application.dto.UserProfileResult;
import com.pikume.back.user.application.port.out.LoadUserPort;
import com.pikume.back.user.application.port.out.UserDiaryPort;
import com.pikume.back.user.application.port.out.UserFriendPort;
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
	private LoadUserPort loadUserPort;
	@Mock
	private UserFriendPort friendPort;
	@Mock
	private UserDiaryPort diaryPort;
	@Mock
	private ImagePathToUrlConverter imagePathToUrlConverter;

	private final RequestMetaInfo meta = new RequestMetaInfo(
			"https", "localhost", 8080, "localhost:8080",
			"https://localhost:8080", "TestAgent", "127.0.0.1");

	@Test
	@DisplayName("프로필 미리보기를 정상 조회한다")
	void getProfilePreview() {
		User user = new User("user-1", "test@test.com", "pw", "피쿠", "avatar.jpg");
		given(loadUserPort.findById("user-1")).willReturn(Optional.of(user));
		given(imagePathToUrlConverter.userAvatarImageUrl(any(), any())).willReturn("https://avatar-url");
		given(friendPort.countFriends("user-1")).willReturn(5);
		given(diaryPort.countDiariesByUserId("user-1")).willReturn(10L);
		given(friendPort.getFriendshipStatus("viewer-1", "user-1")).willReturn("NONE");

		ProfilePreviewResult result = service.getProfilePreview("user-1", "viewer-1", meta);

		assertThat(result.id()).isEqualTo("user-1");
		assertThat(result.nickname()).isEqualTo("피쿠");
		assertThat(result.avatar()).isEqualTo("https://avatar-url");
		assertThat(result.friendCount()).isEqualTo(5);
		assertThat(result.diaryCount()).isEqualTo(10L);
		assertThat(result.friendStatus()).isEqualTo("NONE");
	}

	@Test
	@DisplayName("존재하지 않는 사용자 프로필 조회 시 예외 발생")
	void profileNotFound() {
		given(loadUserPort.findById("unknown")).willReturn(Optional.empty());

		assertThatThrownBy(() -> service.getProfilePreview("unknown", "viewer", meta))
				.isInstanceOf(BusinessException.class);
	}

	@Test
	@DisplayName("프로필 상세 조회 시 소유자 여부와 월별 일기를 포함한다")
	void getUserProfileWithOwnership() {
		User user = new User("user-1", "test@test.com", "pw", "피쿠", "avatar.jpg");
		given(loadUserPort.findById("user-1")).willReturn(Optional.of(user));
		given(imagePathToUrlConverter.userAvatarImageUrl(any(), any())).willReturn("https://avatar-url");
		given(friendPort.countFriends("user-1")).willReturn(3);
		given(diaryPort.countDiariesByUserId("user-1")).willReturn(7L);
		given(friendPort.getFriendshipStatus("user-1", "user-1")).willReturn("NONE");
		given(diaryPort.getMonthlyDiaryCount("user-1")).willReturn(
				List.of(new UserDiaryPort.MonthlyDiaryCount(2026, 1, 5),
						new UserDiaryPort.MonthlyDiaryCount(2026, 2, 2)));

		UserProfileResult result = service.getUserProfile("user-1", "user-1", meta);

		assertThat(result.isOwner()).isTrue();
		assertThat(result.monthlyDiaryCount()).hasSize(2);
	}
}
