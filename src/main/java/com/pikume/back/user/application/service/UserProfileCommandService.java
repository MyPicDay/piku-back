package com.pikume.back.user.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.pikume.back.user.application.dto.UpdateProfileCommand;
import com.pikume.back.user.application.dto.UpdateProfileFailureReason;
import com.pikume.back.user.application.dto.UpdateProfileResult;
import com.pikume.back.user.application.exception.ProfileImageNotFoundException;
import com.pikume.back.user.application.exception.UpdateProfileFailureException;
import com.pikume.back.user.application.exception.UserNotFoundException;
import com.pikume.back.user.application.port.in.CheckNicknameUseCase;
import com.pikume.back.user.application.port.in.UpdateProfileUseCase;
import com.pikume.back.user.application.port.out.LoadFixedCharacterPort;
import com.pikume.back.user.application.port.out.CheckUserUniquenessPort;
import com.pikume.back.user.application.port.out.LoadUserAccountPort;
import com.pikume.back.user.application.port.out.NicknameHoldPort;
import com.pikume.back.user.application.port.out.SaveUserPort;
import com.pikume.back.user.domain.User;

import java.time.Instant;

/**
 * 프로필 수정 Application Service
 * UpdateProfileUseCase와 CheckNicknameUseCase를 구현합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileCommandService implements UpdateProfileUseCase, CheckNicknameUseCase {

	private final LoadUserAccountPort loadUserAccountPort;
	private final SaveUserPort saveUserPort;
	private final CheckUserUniquenessPort checkUserUniquenessPort;
	private final LoadFixedCharacterPort fixedCharacterPort;
	private final NicknameHoldPort nicknameHoldPort;

	@Override
	public boolean checkAvailability(String nickname, String userId) {
		User user = loadUserAccountPort.findById(userId)
				.orElseThrow(UserNotFoundException::new);
		if (nickname.equals(user.getNickname()))
			return true;

		if (checkUserUniquenessPort.existsByNickname(nickname))
			return false;

		return nicknameHoldPort.tryAcquire(nickname, userId, Instant.now());
	}

	@Override
	@Transactional
	public UpdateProfileResult updateProfile(UpdateProfileCommand command) {
		if ((!StringUtils.hasText(command.newNickname())) && (command.characterId() == null)) {
			return UpdateProfileResult.failure(UpdateProfileFailureReason.INVALID_REQUEST, "변경할 닉네임이나 캐릭터 정보가 없습니다.", null);
		}

		User user = loadUserAccountPort.findById(command.userId())
				.orElseThrow(UserNotFoundException::new);
		String oldNickname = user.getNickname();
		String oldAvatarObjectKey = user.getAvatar();

		String targetNickname;
		try {
			targetNickname = getValidatedNewNickname(command.userId(), command.newNickname(), oldNickname);
		} catch (UpdateProfileFailureException e) {
			return UpdateProfileResult.failure(e.getReason(), e.getMessage(), oldNickname);
		}

		String targetAvatarObjectKey;
		try {
			targetAvatarObjectKey = getUpdatedAvatarObjectKey(command.characterId(), oldAvatarObjectKey);
		} catch (UpdateProfileFailureException e) {
			return UpdateProfileResult.failure(e.getReason(), e.getMessage(), oldNickname);
		}

		boolean nicknameChanged = !targetNickname.equals(oldNickname);
		boolean characterChanged = !targetAvatarObjectKey.equals(oldAvatarObjectKey);

		if (!nicknameChanged && !characterChanged) {
			return UpdateProfileResult.success("변경 사항이 없습니다.", oldNickname, targetAvatarObjectKey);
		}

		if (nicknameChanged) {
			user.changeNickname(targetNickname);
		}
		if (characterChanged) {
			user.changeAvatar(targetAvatarObjectKey);
		}
		saveUserPort.save(user);
		if (nicknameChanged) {
			nicknameHoldPort.release(targetNickname, command.userId());
		}

		return buildSuccessResult(nicknameChanged, characterChanged, targetNickname, targetAvatarObjectKey);
	}

	@Override
	@Transactional
	public void updateProfileImage(String userId, Long imageId) {
		User user = loadUserAccountPort.findById(userId)
				.orElseThrow(UserNotFoundException::new);

		String avatarObjectKey = fixedCharacterPort.findFixedCharacterObjectKey(imageId)
				.orElseThrow(() -> {
					log.warn("event=profile_image_update outcome=denied reason=character_not_found characterId={}", imageId);
					return new ProfileImageNotFoundException(imageId);
				});

		user.changeAvatar(avatarObjectKey);
		saveUserPort.save(user);
	}

	private String getValidatedNewNickname(String userId, String newNickname, String oldNickname) {
		if (newNickname == null || newNickname.isEmpty() || newNickname.equals(oldNickname)) {
			return oldNickname;
		}

		if (!nicknameHoldPort.isHeldBy(newNickname, userId, Instant.now())) {
			throw new UpdateProfileFailureException(
					UpdateProfileFailureReason.PROFILE_CONFLICT,
					"닉네임 점유 정보가 없거나 만료되었거나 본인이 아닙니다.");
		}
		if (checkUserUniquenessPort.existsByNickname(newNickname)) {
			throw new UpdateProfileFailureException(
					UpdateProfileFailureReason.NICKNAME_CONFLICT,
					"이미 사용 중인 닉네임입니다.");
		}
		return newNickname;
	}

	private String getUpdatedAvatarObjectKey(Long characterId, String oldAvatarObjectKey) {
		if (characterId == null) {
			return oldAvatarObjectKey;
		}
		if (characterId <= 0) {
			log.warn("event=profile_update outcome=denied reason=invalid_character_id characterId={}", characterId);
			throw new UpdateProfileFailureException(
					UpdateProfileFailureReason.INVALID_REQUEST,
					"유효하지 않은 캐릭터 ID입니다.");
		}
		String newAvatarObjectKey = fixedCharacterPort.findFixedCharacterObjectKey(characterId)
				.orElseThrow(() -> {
					log.warn("event=profile_update outcome=denied reason=character_not_found characterId={}", characterId);
					return new UpdateProfileFailureException(
							UpdateProfileFailureReason.RESOURCE_NOT_FOUND,
							"존재하지 않는 캐릭터입니다.");
				});
		return newAvatarObjectKey.equals(oldAvatarObjectKey) ? oldAvatarObjectKey : newAvatarObjectKey;
	}

	private UpdateProfileResult buildSuccessResult(boolean nicknameChanged, boolean characterChanged, String nickname,
			String avatar) {
		String message;
		if (nicknameChanged && characterChanged) {
			message = "닉네임과 캐릭터가 성공적으로 변경되었습니다.";
		} else if (nicknameChanged) {
			message = "닉네임이 성공적으로 변경되었습니다.";
		} else {
			message = "캐릭터가 성공적으로 변경되었습니다.";
		}
		return UpdateProfileResult.success(message, nickname, characterChanged ? avatar : null);
	}
}
