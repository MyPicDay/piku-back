package com.pikume.back.user.adapter.in.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.ProblemDetail;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import com.pikume.back.global.config.CustomUserDetails;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.user.adapter.in.web.dto.request.UpdateProfileRequest;
import com.pikume.back.user.adapter.in.web.dto.response.ProfilePreviewResponse;
import com.pikume.back.user.adapter.in.web.dto.response.UserProfileResponse;
import com.pikume.back.user.application.dto.ProfilePreviewResult;
import com.pikume.back.user.application.dto.UpdateProfileFailureReason;
import com.pikume.back.user.application.dto.UpdateProfileResult;
import com.pikume.back.user.application.dto.UserProfileResult;
import com.pikume.back.user.application.exception.ProfileImageNotFoundException;
import com.pikume.back.user.application.port.in.CheckNicknameUseCase;
import com.pikume.back.user.application.port.in.GetUserProfileUseCase;
import com.pikume.back.user.application.port.in.UpdateProfileUseCase;
import com.pikume.back.user.application.port.out.UserDiaryPort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController")
class UserControllerTest {

	@Mock
	private GetUserProfileUseCase getUserProfileUseCase;

	@Mock
	private UpdateProfileUseCase updateProfileUseCase;

	@Mock
	private CheckNicknameUseCase checkNicknameUseCase;

	private UserController userController;

	@BeforeEach
	void setUp() {
		userController = new UserController(
				getUserProfileUseCase,
				updateProfileUseCase,
				checkNicknameUseCase,
				new ProblemDetailFactory());
	}

	@Test
	@DisplayName("GET /api/users/{userId}/profile-preview는 비로그인 요청에도 프로필 미리보기를 반환한다")
	void getProfilePreviewReturnsPublicPreviewWithoutAuthenticatedUser() {
		ProfilePreviewResult result = new ProfilePreviewResult(
				"user1",
				"pikume",
				"https://assets.example.com/avatar.webp",
				3,
				12L,
				"NONE");
		given(getUserProfileUseCase.getProfilePreview("user1", null)).willReturn(result);

		ResponseEntity<?> response = userController.getProfilePreview("user1", null);

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getBody()).isEqualTo(ProfilePreviewResponse.from(result));
	}

	@Test
	@DisplayName("GET /api/users/{userId}는 인증 사용자 기준의 상세 프로필 계약을 반환한다")
	void getUserProfileReturnsAuthenticatedProfileContract() {
		UserProfileResult result = new UserProfileResult(
				"user1",
				"pikume",
				"https://assets.example.com/avatar.webp",
				3,
				12L,
				"FRIEND",
				true,
				List.of(new UserDiaryPort.MonthlyDiaryCount(2026, 7, 4L)));
		given(getUserProfileUseCase.getUserProfile("user1", "user1")).willReturn(result);

		ResponseEntity<?> response = userController.getUserProfile(
				"user1",
				new CustomUserDetails("user1", "pikume"));

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getBody()).isEqualTo(UserProfileResponse.from(result));
	}

	@Test
	@DisplayName("PUT /api/users/profile-image는 이미지가 없으면 404 Problem Details를 반환한다")
	void updateProfileImageReturnsProblemDetailWhenImageDoesNotExist() {
		org.mockito.BDDMockito.willThrow(new ProfileImageNotFoundException(10L))
				.given(updateProfileUseCase)
				.updateProfileImage("user1", 10L);

		ResponseEntity<?> response = userController.updateProfileImage(
				new CustomUserDetails("user1", "pikume"),
				10L);

		assertThat(response.getStatusCode().value()).isEqualTo(404);
		assertThat(response.getBody()).isInstanceOf(org.springframework.http.ProblemDetail.class);
		org.springframework.http.ProblemDetail problemDetail = (org.springframework.http.ProblemDetail) response.getBody();
		assertThat(problemDetail.getType().toString()).isEqualTo("https://api.pikume.com/problems/common/resource-not-found");
		assertThat(problemDetail.getStatus()).isEqualTo(404);
		assertThat(problemDetail.getInstance().toString()).isEqualTo("/api/users/profile-image");
	}

	@Test
	@DisplayName("PUT /api/users/profile-image는 성공 시 200을 반환한다")
	void updateProfileImageReturnsOkWhenImageExists() {
		ResponseEntity<?> response = userController.updateProfileImage(
				new CustomUserDetails("user1", "pikume"),
				1L);

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getBody()).isNull();
	}

	@Test
	@DisplayName("GET /api/users/nickname/availability는 충돌 시 409 Problem Details를 반환한다")
	void checkNicknameReturnsProblemDetailWhenNicknameConflicts() {
		given(checkNicknameUseCase.checkAvailability("taken", "user1")).willReturn(false);

		ResponseEntity<?> response = userController.checkNickname(
				"taken",
				new CustomUserDetails("user1", "pikume"));

		assertThat(response.getStatusCode().value()).isEqualTo(409);
		assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
		ProblemDetail problemDetail = (ProblemDetail) response.getBody();
		assertThat(problemDetail.getType().toString()).isEqualTo("https://api.pikume.com/problems/user/nickname-conflict");
		assertThat(problemDetail.getStatus()).isEqualTo(409);
		assertThat(problemDetail.getDetail()).isEqualTo("이미 사용 중인 닉네임입니다.");
		assertThat(problemDetail.getInstance().toString()).isEqualTo("/api/users/nickname/availability");
	}

	@Test
	@DisplayName("PATCH /api/users/profile는 충돌 시 409 Problem Details를 반환한다")
	void changeNicknameReturnsProblemDetailWhenProfileConflicts() {
		given(updateProfileUseCase.updateProfile(org.mockito.ArgumentMatchers.any()))
				.willReturn(UpdateProfileResult.failure(
						UpdateProfileFailureReason.PROFILE_CONFLICT,
						"점유 정보가 없거나 만료되었거나 본인이 아닙니다.",
						"old-nickname"));

		ResponseEntity<?> response = userController.changeNickname(
				new CustomUserDetails("user1", "pikume"),
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
		given(updateProfileUseCase.updateProfile(org.mockito.ArgumentMatchers.any()))
				.willReturn(UpdateProfileResult.failure(
						UpdateProfileFailureReason.NICKNAME_CONFLICT,
						"이미 사용 중인 닉네임입니다.",
						"old-nickname"));

		ResponseEntity<?> response = userController.changeNickname(
				new CustomUserDetails("user1", "pikume"),
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
		given(updateProfileUseCase.updateProfile(org.mockito.ArgumentMatchers.any()))
				.willReturn(UpdateProfileResult.failure(
						UpdateProfileFailureReason.INVALID_REQUEST,
						"변경할 닉네임이나 캐릭터 정보가 없습니다.",
						"old-nickname"));

		ResponseEntity<?> response = userController.changeNickname(
				new CustomUserDetails("user1", "pikume"),
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
		given(updateProfileUseCase.updateProfile(org.mockito.ArgumentMatchers.any()))
				.willReturn(UpdateProfileResult.failure(
						UpdateProfileFailureReason.RESOURCE_NOT_FOUND,
						"존재하지 않는 캐릭터입니다.",
						"old-nickname"));

		ResponseEntity<?> response = userController.changeNickname(
				new CustomUserDetails("user1", "pikume"),
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
