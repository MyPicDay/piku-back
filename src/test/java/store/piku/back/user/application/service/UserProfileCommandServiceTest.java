package store.piku.back.user.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import store.piku.back.global.error.ErrorCode;
import store.piku.back.global.exception.BusinessException;
import store.piku.back.user.application.port.out.LoadCharacterPort;
import store.piku.back.user.application.port.out.LoadUserPort;
import store.piku.back.user.application.port.out.SaveUserPort;
import store.piku.back.user.application.port.out.UserQueryPort;
import store.piku.back.user.application.dto.UpdateProfileCommand;
import store.piku.back.user.application.dto.UpdateProfileResult;
import store.piku.back.user.domain.User;
import store.piku.back.user.domain.service.NicknamePolicy;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileCommandService")
class UserProfileCommandServiceTest {

	@InjectMocks
	private UserProfileCommandService service;

	@Mock
	private LoadUserPort loadUserPort;
	@Mock
	private SaveUserPort saveUserPort;
	@Mock
	private UserQueryPort userQueryPort;
	@Mock
	private LoadCharacterPort characterPort;
	@Mock
	private NicknamePolicy nicknamePolicy;

	@Nested
	@DisplayName("checkAvailability - 닉네임 사용 가능 확인")
	class CheckAvailability {

		@Test
		@DisplayName("현재 자신의 닉네임이면 사용 가능")
		void ownNicknameIsAvailable() {
			User user = new User("user-1", "test@test.com", "pw", "현재닉", "avatar");
			given(loadUserPort.findById("user-1")).willReturn(Optional.of(user));

			boolean result = service.checkAvailability("현재닉", "user-1");

			assertThat(result).isTrue();
		}

		@Test
		@DisplayName("이미 DB에 존재하는 닉네임이면 사용 불가")
		void existingNicknameIsUnavailable() {
			User user = new User("user-1", "test@test.com", "pw", "현재닉", "avatar");
			given(loadUserPort.findById("user-1")).willReturn(Optional.of(user));
			given(userQueryPort.existsByNickname("중복닉")).willReturn(true);

			boolean result = service.checkAvailability("중복닉", "user-1");

			assertThat(result).isFalse();
		}

		@Test
		@DisplayName("존재하지 않는 사용자면 예외 발생")
		void nonExistentUserThrows() {
			given(loadUserPort.findById("unknown")).willReturn(Optional.empty());

			assertThatThrownBy(() -> service.checkAvailability("닉네임", "unknown"))
					.isInstanceOf(BusinessException.class);
		}
	}

	@Nested
	@DisplayName("updateProfile - 프로필 변경")
	class UpdateProfile {

		@Test
		@DisplayName("닉네임과 캐릭터 모두 없으면 실패 응답")
		void noChangesReturnFailure() {
			UpdateProfileCommand command = new UpdateProfileCommand("user-1", null, null);

			UpdateProfileResult result = service.updateProfile(command);

			assertThat(result.success()).isFalse();
		}

		@Test
		@DisplayName("존재하지 않는 사용자면 예외 발생")
		void nonExistentUserThrows() {
			UpdateProfileCommand command = new UpdateProfileCommand("unknown", "새닉", null);
			given(loadUserPort.findById("unknown")).willReturn(Optional.empty());

			assertThatThrownBy(() -> service.updateProfile(command))
					.isInstanceOf(BusinessException.class);
		}
	}

	@Nested
	@DisplayName("updateProfileImage - 프로필 이미지 변경")
	class UpdateProfileImage {

		@Test
		@DisplayName("유효한 캐릭터 ID로 프로필 이미지를 변경한다")
		void validCharacterIdUpdatesImage() {
			User user = new User("user-1", "test@test.com", "pw", "닉네임", "old-avatar");
			given(loadUserPort.findById("user-1")).willReturn(Optional.of(user));
			given(characterPort.getFixedCharacterImageUrl(1L)).willReturn("new-avatar-url");
			given(saveUserPort.save(any(User.class))).willReturn(user);

			boolean result = service.updateProfileImage("user-1", 1L);

			assertThat(result).isTrue();
			verify(saveUserPort).save(any(User.class));
		}

		@Test
		@DisplayName("존재하지 않는 캐릭터 이미지면 실패")
		void nonExistentCharacterReturnsFalse() {
			User user = new User("user-1", "test@test.com", "pw", "닉네임", "avatar");
			given(loadUserPort.findById("user-1")).willReturn(Optional.of(user));
			given(characterPort.getFixedCharacterImageUrl(999L)).willReturn(null);

			boolean result = service.updateProfileImage("user-1", 999L);

			assertThat(result).isFalse();
			verify(saveUserPort, never()).save(any());
		}
	}
}
