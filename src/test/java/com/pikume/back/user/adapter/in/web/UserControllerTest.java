package com.pikume.back.user.adapter.in.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.ProblemDetail;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import com.pikume.back.security.principal.UserPrincipal;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.global.port.out.ResolveObjectUrlPort;
import com.pikume.back.user.adapter.in.web.dto.request.UpdateProfileRequest;
import com.pikume.back.user.adapter.in.web.dto.response.NicknameChangeResponse;
import com.pikume.back.user.adapter.in.web.dto.response.ProfilePreviewResponse;
import com.pikume.back.user.adapter.in.web.dto.response.UserProfileResponse;
import com.pikume.back.user.application.dto.ProfilePreviewResult;
import com.pikume.back.user.application.dto.UpdateProfileFailureReason;
import com.pikume.back.user.application.dto.UpdateProfileResult;
import com.pikume.back.user.application.dto.UserProfileResult;
import com.pikume.back.user.application.dto.UserAvatarReference;
import com.pikume.back.user.application.exception.ProfileImageNotFoundException;
import com.pikume.back.user.application.port.in.ReserveNicknameUseCase;
import com.pikume.back.user.application.port.in.QueryUserProfileUseCase;
import com.pikume.back.user.application.port.in.UpdateUserProfileUseCase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController")
class UserControllerTest {

	@Mock
	private QueryUserProfileUseCase queryUserProfileUseCase;

	@Mock
	private UpdateUserProfileUseCase updateUserProfileUseCase;

	@Mock
	private ReserveNicknameUseCase reserveNicknameUseCase;

	@Mock
	private ResolveObjectUrlPort resolveObjectUrlPort;

	private UserController userController;

	@BeforeEach
	void setUp() {
		userController = new UserController(
				queryUserProfileUseCase,
				updateUserProfileUseCase,
				reserveNicknameUseCase,
				new ProblemDetailFactory(),
				resolveObjectUrlPort);
	}

	@Test
	@DisplayName("GET /api/users/{userId}/profile-preview는 비로그인 요청에도 프로필 미리보기를 반환한다")
	void queryProfilePreviewReturnsPublicPreviewWithoutAuthenticatedUser() {
		ProfilePreviewResult result = new ProfilePreviewResult(
				"user1",
				"pikume",
				new UserAvatarReference("avatar-object-key", false, true),
				3,
				12L,
				"NONE");
		given(queryUserProfileUseCase.queryProfilePreview("user1", null)).willReturn(result);
		given(resolveObjectUrlPort.resolveObjectUrl(
				"avatar-object-key",
				true))
				.willReturn("https://assets.example.com/avatar.webp");

		ResponseEntity<?> response = userController.queryProfilePreview("user1", null);

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getBody()).isEqualTo(ProfilePreviewResponse.from(result, resolveObjectUrlPort));
	}

	@Test
	@DisplayName("GET /api/users/{userId}는 인증 사용자 기준의 상세 프로필 계약을 반환한다")
	void queryUserProfileReturnsAuthenticatedProfileContract() {
		UserProfileResult result = new UserProfileResult(
				"user1",
				"pikume",
				new UserAvatarReference("avatar-object-key", false, true),
				3,
				12L,
				"FRIEND",
				true,
				List.of(new UserProfileResult.MonthlyDiaryCount(2026, 7, 4L)));
		given(queryUserProfileUseCase.queryUserProfile("user1", "user1")).willReturn(result);
		given(resolveObjectUrlPort.resolveObjectUrl(
				"avatar-object-key",
				true))
				.willReturn("https://assets.example.com/avatar.webp");

		ResponseEntity<?> response = userController.queryUserProfile(
				"user1",
				new UserPrincipal("user1", "pikume"));

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getBody()).isEqualTo(UserProfileResponse.from(result, resolveObjectUrlPort));
	}

	@Test
	@DisplayName("PUT /api/users/profile-image는 이미지가 없으면 404 Problem Details를 반환한다")
	void updateProfileImageReturnsProblemDetailWhenImageDoesNotExist() {
		org.mockito.BDDMockito.willThrow(new ProfileImageNotFoundException(10L))
				.given(updateUserProfileUseCase)
				.updateProfileImage("user1", 10L);

		org.assertj.core.api.Assertions.assertThatThrownBy(() -> userController.updateProfileImage(
				new UserPrincipal("user1", "pikume"), 10L))
				.isInstanceOf(ProfileImageNotFoundException.class);
	}

	@Test
	@DisplayName("PUT /api/users/profile-image는 성공 시 200을 반환한다")
	void updateProfileImageReturnsOkWhenImageExists() {
		ResponseEntity<?> response = userController.updateProfileImage(
				new UserPrincipal("user1", "pikume"),
				1L);

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getBody()).isNull();
	}

	@Test
	@DisplayName("GET /api/users/nickname/availability는 충돌 시 409 Problem Details를 반환한다")
	void checkNicknameReturnsProblemDetailWhenNicknameConflicts() {
		given(reserveNicknameUseCase.reserveIfAvailable("taken", "user1")).willReturn(false);

		ResponseEntity<?> response = userController.checkNickname(
				"taken",
				new UserPrincipal("user1", "pikume"));

		assertThat(response.getStatusCode().value()).isEqualTo(409);
		assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
		ProblemDetail problemDetail = (ProblemDetail) response.getBody();
		assertThat(problemDetail.getType().toString()).isEqualTo("https://api.pikume.com/problems/user/nickname-conflict");
		assertThat(problemDetail.getStatus()).isEqualTo(409);
		assertThat(problemDetail.getDetail()).isEqualTo("이미 사용 중인 닉네임입니다.");
		assertThat(problemDetail.getInstance().toString()).isEqualTo("/api/users/nickname/availability");
	}

	@Test
	@DisplayName("PATCH /api/users/profile는 변경된 아바타를 스토리지 URL로 반환한다")
	void changeNicknameReturnsResolvedAvatarUrl() {
		given(updateUserProfileUseCase.updateProfile(org.mockito.ArgumentMatchers.any()))
				.willReturn(UpdateProfileResult.success(
						"캐릭터가 성공적으로 변경되었습니다.",
						"pikume",
						"public/characters/fixed/base_image_2.webp"));
		given(resolveObjectUrlPort.resolveObjectUrl(
				"public/characters/fixed/base_image_2.webp",
				true))
				.willReturn("https://assets.example.com/piku/public/characters/fixed/base_image_2.webp");

		ResponseEntity<?> response = userController.changeNickname(
				new UserPrincipal("user1", "pikume"),
				new UpdateProfileRequest(null, 2L));

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getBody()).isEqualTo(new NicknameChangeResponse(
				true,
				"캐릭터가 성공적으로 변경되었습니다.",
				"pikume",
				"https://assets.example.com/piku/public/characters/fixed/base_image_2.webp"));
	}

	@Test
	@DisplayName("PATCH /api/users/profile는 절대 아바타 URL을 변경하지 않는다")
	void changeNicknamePreservesAbsoluteAvatarUrl() {
		given(updateUserProfileUseCase.updateProfile(org.mockito.ArgumentMatchers.any()))
				.willReturn(UpdateProfileResult.success(
						"캐릭터가 성공적으로 변경되었습니다.",
						"pikume",
						"https://legacy-assets.example.com/base_image_2.webp"));

		ResponseEntity<?> response = userController.changeNickname(
				new UserPrincipal("user1", "pikume"),
				new UpdateProfileRequest(null, 2L));

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getBody()).isEqualTo(new NicknameChangeResponse(
				true,
				"캐릭터가 성공적으로 변경되었습니다.",
				"pikume",
				"https://legacy-assets.example.com/base_image_2.webp"));
	}

	@Test
	@DisplayName("PATCH /api/users/profile는 닉네임만 변경하면 아바타 URL을 반환하지 않는다")
	void changeNicknameWithoutCharacterReturnsNullAvatar() {
		given(updateUserProfileUseCase.updateProfile(org.mockito.ArgumentMatchers.any()))
				.willReturn(UpdateProfileResult.success(
						"닉네임이 성공적으로 변경되었습니다.",
						"new-nickname",
						null));

		ResponseEntity<?> response = userController.changeNickname(
				new UserPrincipal("user1", "pikume"),
				new UpdateProfileRequest("new-nickname", null));

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getBody()).isEqualTo(new NicknameChangeResponse(
				true,
				"닉네임이 성공적으로 변경되었습니다.",
				"new-nickname",
				null));
		then(resolveObjectUrlPort).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("PATCH /api/users/profile는 충돌 시 409 Problem Details를 반환한다")
	void changeNicknameReturnsProblemDetailWhenProfileConflicts() {
		given(updateUserProfileUseCase.updateProfile(org.mockito.ArgumentMatchers.any()))
				.willReturn(UpdateProfileResult.failure(
						UpdateProfileFailureReason.PROFILE_CONFLICT,
						"점유 정보가 없거나 만료되었거나 본인이 아닙니다.",
						"old-nickname"));

		ResponseEntity<?> response = userController.changeNickname(
				new UserPrincipal("user1", "pikume"),
				new UpdateProfileRequest("new-nickname", 1L));

		assertThat(response.getStatusCode().value()).isEqualTo(409);
		assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
		ProblemDetail problemDetail = (ProblemDetail) response.getBody();
		assertThat(problemDetail.getType().toString()).isEqualTo("https://api.pikume.com/problems/user/profile-conflict");
		assertThat(problemDetail.getStatus()).isEqualTo(409);
		assertThat(problemDetail.getDetail()).isEqualTo("점유 정보가 없거나 만료되었거나 본인이 아닙니다.");
		assertThat(problemDetail.getInstance().toString()).isEqualTo("/api/users/profile");
	}

	@Test
	@DisplayName("PATCH /api/users/profile는 닉네임 충돌 시 409 nickname-conflict를 반환한다")
	void changeNicknameReturnsNicknameConflictProblemDetail() {
		given(updateUserProfileUseCase.updateProfile(org.mockito.ArgumentMatchers.any()))
				.willReturn(UpdateProfileResult.failure(
						UpdateProfileFailureReason.NICKNAME_CONFLICT,
						"이미 사용 중인 닉네임입니다.",
						"old-nickname"));

		ResponseEntity<?> response = userController.changeNickname(
				new UserPrincipal("user1", "pikume"),
				new UpdateProfileRequest("taken", null));

		assertThat(response.getStatusCode().value()).isEqualTo(409);
		assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
		ProblemDetail problemDetail = (ProblemDetail) response.getBody();
		assertThat(problemDetail.getType().toString()).isEqualTo("https://api.pikume.com/problems/user/nickname-conflict");
		assertThat(problemDetail.getStatus()).isEqualTo(409);
		assertThat(problemDetail.getDetail()).isEqualTo("이미 사용 중인 닉네임입니다.");
		assertThat(problemDetail.getInstance().toString()).isEqualTo("/api/users/profile");
	}

	@Test
	@DisplayName("PATCH /api/users/profile는 잘못된 요청이면 400 Problem Details를 반환한다")
	void changeNicknameReturnsBadRequestProblemDetailWhenRequestIsInvalid() {
		given(updateUserProfileUseCase.updateProfile(org.mockito.ArgumentMatchers.any()))
				.willReturn(UpdateProfileResult.failure(
						UpdateProfileFailureReason.INVALID_REQUEST,
						"변경할 닉네임이나 캐릭터 정보가 없습니다.",
						"old-nickname"));

		ResponseEntity<?> response = userController.changeNickname(
				new UserPrincipal("user1", "pikume"),
				new UpdateProfileRequest(null, null));

		assertThat(response.getStatusCode().value()).isEqualTo(400);
		assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
		ProblemDetail problemDetail = (ProblemDetail) response.getBody();
		assertThat(problemDetail.getType().toString()).isEqualTo("https://api.pikume.com/problems/validation/invalid-request");
		assertThat(problemDetail.getStatus()).isEqualTo(400);
		assertThat(problemDetail.getDetail()).isEqualTo("변경할 닉네임이나 캐릭터 정보가 없습니다.");
		assertThat(problemDetail.getInstance().toString()).isEqualTo("/api/users/profile");
	}

	@Test
	@DisplayName("PATCH /api/users/profile는 참조 리소스가 없으면 404 Problem Details를 반환한다")
	void changeNicknameReturnsNotFoundProblemDetailWhenResourceDoesNotExist() {
		given(updateUserProfileUseCase.updateProfile(org.mockito.ArgumentMatchers.any()))
				.willReturn(UpdateProfileResult.failure(
						UpdateProfileFailureReason.RESOURCE_NOT_FOUND,
						"존재하지 않는 캐릭터입니다.",
						"old-nickname"));

		ResponseEntity<?> response = userController.changeNickname(
				new UserPrincipal("user1", "pikume"),
				new UpdateProfileRequest("new-nickname", 999L));

		assertThat(response.getStatusCode().value()).isEqualTo(404);
		assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
		ProblemDetail problemDetail = (ProblemDetail) response.getBody();
		assertThat(problemDetail.getType().toString()).isEqualTo("https://api.pikume.com/problems/common/resource-not-found");
		assertThat(problemDetail.getStatus()).isEqualTo(404);
		assertThat(problemDetail.getDetail()).isEqualTo("존재하지 않는 캐릭터입니다.");
		assertThat(problemDetail.getInstance().toString()).isEqualTo("/api/users/profile");
	}
}
