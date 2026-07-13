package com.pikume.back.user.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.pikume.back.user.application.port.out.LoadFixedCharacterPort;
import com.pikume.back.user.application.port.out.CheckUserUniquenessPort;
import com.pikume.back.user.application.port.out.LoadUserAccountPort;
import com.pikume.back.user.application.port.out.NicknameHoldPort;
import com.pikume.back.user.application.port.out.SaveUserPort;
import com.pikume.back.user.application.dto.UpdateProfileCommand;
import com.pikume.back.user.application.dto.UpdateProfileFailureReason;
import com.pikume.back.user.application.dto.UpdateProfileResult;
import com.pikume.back.user.application.exception.ProfileImageNotFoundException;
import com.pikume.back.user.application.exception.UserErrorCode;
import com.pikume.back.user.application.exception.UserNotFoundException;
import com.pikume.back.user.domain.User;
import com.pikume.back.user.domain.exception.NicknameAlreadyExistsException;
import com.pikume.back.user.domain.service.NicknamePolicy;

import java.util.Optional;
import java.time.Instant;

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
	private LoadUserAccountPort loadUserAccountPort;
	@Mock
	private SaveUserPort saveUserPort;
	@Mock
	private CheckUserUniquenessPort checkUserUniquenessPort;
	@Mock
	private NicknameHoldPort nicknameHoldPort;
	@Mock
	private LoadFixedCharacterPort fixedCharacterPort;
	@Mock
	private NicknamePolicy nicknamePolicy;

	@Nested
	@DisplayName("checkAvailability - 닉네임 사용 가능 확인")
	class CheckAvailability {

		@Test
		@DisplayName("현재 자신의 닉네임이면 사용 가능")
		void ownNicknameIsAvailable() {
			User user = new User("user-1", "test@test.com", "pw", "현재닉", "avatar");
			given(loadUserAccountPort.findById("user-1")).willReturn(Optional.of(user));

			boolean result = service.checkAvailability("현재닉", "user-1");

			assertThat(result).isTrue();
		}

		@Test
		@DisplayName("이미 DB에 존재하는 닉네임이면 사용 불가")
		void existingNicknameIsUnavailable() {
			User user = new User("user-1", "test@test.com", "pw", "현재닉", "avatar");
			given(loadUserAccountPort.findById("user-1")).willReturn(Optional.of(user));
			given(checkUserUniquenessPort.existsByNickname("중복닉")).willReturn(true);

			boolean result = service.checkAvailability("중복닉", "user-1");

			assertThat(result).isFalse();
		}

		@Test
		@DisplayName("사용 가능한 닉네임 점유를 목적 중심 Port에 위임한다")
		void delegatesNicknameAcquisitionToHoldPort() {
			User user = new User("user-1", "test@test.com", "pw", "현재닉", "avatar");
			given(loadUserAccountPort.findById("user-1")).willReturn(Optional.of(user));
			given(nicknameHoldPort.tryAcquire(eq("새닉"), eq("user-1"), any(Instant.class))).willReturn(true);

			assertThat(service.checkAvailability("새닉", "user-1")).isTrue();
		}

		@Test
		@DisplayName("존재하지 않는 사용자면 예외 발생")
		void nonExistentUserThrows() {
			given(loadUserAccountPort.findById("unknown")).willReturn(Optional.empty());

				assertThatThrownBy(() -> service.checkAvailability("닉네임", "unknown"))
						.isInstanceOfSatisfying(UserNotFoundException.class,
								ex -> assertThat(ex.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND));
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
			assertThat(result.failureReason()).isEqualTo(UpdateProfileFailureReason.INVALID_REQUEST);
		}

		@Test
		@DisplayName("존재하지 않는 사용자면 예외 발생")
		void nonExistentUserThrows() {
			UpdateProfileCommand command = new UpdateProfileCommand("unknown", "새닉", null);
			given(loadUserAccountPort.findById("unknown")).willReturn(Optional.empty());

				assertThatThrownBy(() -> service.updateProfile(command))
						.isInstanceOfSatisfying(UserNotFoundException.class,
								ex -> assertThat(ex.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND));
		}

		@Test
		@DisplayName("존재하지 않는 캐릭터면 RESOURCE_NOT_FOUND 실패 응답")
		void nonExistentCharacterReturnsResourceNotFoundFailure() {
			User user = new User("user-1", "test@test.com", "pw", "닉네임", "old-avatar");
			UpdateProfileCommand command = new UpdateProfileCommand("user-1", null, 999L);
			given(loadUserAccountPort.findById("user-1")).willReturn(Optional.of(user));
			given(fixedCharacterPort.findFixedCharacterObjectKey(999L)).willReturn(Optional.empty());

			UpdateProfileResult result = service.updateProfile(command);

			assertThat(result.success()).isFalse();
			assertThat(result.failureReason()).isEqualTo(UpdateProfileFailureReason.RESOURCE_NOT_FOUND);
			assertThat(result.message()).isEqualTo("존재하지 않는 캐릭터입니다.");
		}

		@Test
		@DisplayName("캐릭터 변경 시 canonical object key를 아바타로 저장한다")
		void storesCanonicalObjectKeyWhenCharacterChanges() {
			User user = new User("user-1", "test@test.com", "pw", "닉네임", "old-avatar");
			UpdateProfileCommand command = new UpdateProfileCommand("user-1", null, 1L);
			given(loadUserAccountPort.findById("user-1")).willReturn(Optional.of(user));
			given(fixedCharacterPort.findFixedCharacterObjectKey(1L))
					.willReturn(Optional.of("public/characters/fixed/base_image_1.webp"));
			given(saveUserPort.save(any(User.class))).willReturn(user);

			UpdateProfileResult result = service.updateProfile(command);

			assertThat(result.success()).isTrue();
			assertThat(result.avatar()).isEqualTo("public/characters/fixed/base_image_1.webp");
			verify(saveUserPort).save(same(user));
			verify(saveUserPort).save(argThat(savedUser ->
					"public/characters/fixed/base_image_1.webp".equals(savedUser.getAvatar())));
		}

		@Test
		@DisplayName("닉네임 변경 성공 후 점유를 해제한다")
		void releasesNicknameHoldAfterSuccessfulUpdate() {
			User user = new User("user-1", "test@test.com", "pw", "현재닉", "avatar");
			UpdateProfileCommand command = new UpdateProfileCommand("user-1", "새닉", null);
			given(loadUserAccountPort.findById("user-1")).willReturn(Optional.of(user));
			given(nicknameHoldPort.isHeldBy(eq("새닉"), eq("user-1"), any(Instant.class))).willReturn(true);
			given(saveUserPort.save(user)).willReturn(user);

			UpdateProfileResult result = service.updateProfile(command);

			assertThat(result.success()).isTrue();
			verify(nicknameHoldPort).release("새닉", "user-1");
		}

		@Test
		@DisplayName("닉네임 저장 충돌 시 예외를 전파하고 점유를 유지한다")
		void keepsNicknameHoldAfterPersistenceConflict() {
			User user = new User("user-1", "test@test.com", "pw", "현재닉", "avatar");
			UpdateProfileCommand command = new UpdateProfileCommand("user-1", "새닉", null);
			given(loadUserAccountPort.findById("user-1")).willReturn(Optional.of(user));
			given(nicknameHoldPort.isHeldBy(eq("새닉"), eq("user-1"), any(Instant.class))).willReturn(true);
			given(saveUserPort.save(user)).willThrow(new NicknameAlreadyExistsException("새닉"));

			assertThatThrownBy(() -> service.updateProfile(command))
					.isInstanceOf(NicknameAlreadyExistsException.class)
					.hasMessageContaining("새닉");
			verify(nicknameHoldPort, never()).release(anyString(), anyString());
		}
	}

	@Nested
	@DisplayName("updateProfileImage - 프로필 이미지 변경")
	class UpdateProfileImage {

		@Test
		@DisplayName("유효한 캐릭터 ID로 프로필 이미지를 변경한다")
		void validCharacterIdUpdatesImage() {
			User user = new User("user-1", "test@test.com", "pw", "닉네임", "old-avatar");
			given(loadUserAccountPort.findById("user-1")).willReturn(Optional.of(user));
			given(fixedCharacterPort.findFixedCharacterObjectKey(1L))
					.willReturn(Optional.of("public/characters/fixed/base_image_1.webp"));
			given(saveUserPort.save(any(User.class))).willReturn(user);

			service.updateProfileImage("user-1", 1L);

			verify(saveUserPort).save(argThat(savedUser ->
					"public/characters/fixed/base_image_1.webp".equals(savedUser.getAvatar())));
		}

		@Test
		@DisplayName("존재하지 않는 캐릭터 이미지면 ProfileImageNotFoundException 발생")
		void nonExistentCharacterThrowsNotFound() {
			User user = new User("user-1", "test@test.com", "pw", "닉네임", "avatar");
			given(loadUserAccountPort.findById("user-1")).willReturn(Optional.of(user));
			given(fixedCharacterPort.findFixedCharacterObjectKey(999L)).willReturn(Optional.empty());

			assertThatThrownBy(() -> service.updateProfileImage("user-1", 999L))
					.isInstanceOf(ProfileImageNotFoundException.class);
			verify(saveUserPort, never()).save(any());
		}
	}
}
