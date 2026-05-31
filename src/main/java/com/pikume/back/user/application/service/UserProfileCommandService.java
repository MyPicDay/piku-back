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
import com.pikume.back.user.application.port.out.LoadCharacterPort;
import com.pikume.back.user.application.port.out.LoadUserPort;
import com.pikume.back.user.application.port.out.SaveUserPort;
import com.pikume.back.user.application.port.out.UserQueryPort;
import com.pikume.back.user.domain.User;
import com.pikume.back.user.domain.service.NicknamePolicy;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 프로필 수정 Application Service
 * UpdateProfileUseCase와 CheckNicknameUseCase를 구현합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileCommandService implements UpdateProfileUseCase, CheckNicknameUseCase {

	private final LoadUserPort loadUserPort;
	private final SaveUserPort saveUserPort;
	private final UserQueryPort userQueryPort;
	private final LoadCharacterPort characterPort;
	private final NicknamePolicy nicknamePolicy;

	// NicknameHold를 내부 record로 관리
	private final ConcurrentHashMap<String, NicknameHoldEntry> nicknameHoldMap = new ConcurrentHashMap<>();

	record NicknameHoldEntry(String userId, long timestamp) {
	}

	@Override
	public boolean checkAvailability(String nickname, String userId) {
		long now = System.currentTimeMillis();

        User user = loadUserPort.findById(userId)
                .orElseThrow(UserNotFoundException::new);
		if (nickname.equals(user.getNickname()))
			return true;

		if (userQueryPort.existsByNickname(nickname))
			return false;

		boolean reserved = nicknameHoldMap.compute(nickname, (key, hold) -> {
			if (hold == null || nicknamePolicy.isHoldExpired(hold.timestamp(), now)) {
				return new NicknameHoldEntry(userId, now);
			}
			return hold;
		}).userId().equals(userId);

		return reserved;
	}

	@Override
	@Transactional
	public UpdateProfileResult updateProfile(UpdateProfileCommand command) {
		if ((!StringUtils.hasText(command.newNickname())) && (command.characterId() == null)) {
			return UpdateProfileResult.failure(UpdateProfileFailureReason.INVALID_REQUEST, "변경할 닉네임이나 캐릭터 정보가 없습니다.", null);
		}

		User user = loadUserPort.findById(command.userId())
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

		try {
			User updatedUser = new User(
					user.getId(),
					user.getEmail(),
					user.getPassword(),
					targetNickname,
					targetAvatarObjectKey);
			saveUserPort.save(updatedUser);

			return buildSuccessResult(nicknameChanged, characterChanged, targetNickname, targetAvatarObjectKey);
		} finally {
			if (nicknameChanged) {
				nicknameHoldMap.remove(command.newNickname());
			}
		}
	}

	@Override
	@Transactional
	public void updateProfileImage(String userId, Long imageId) {
		User user = loadUserPort.findById(userId)
				.orElseThrow(UserNotFoundException::new);

		String avatarObjectKey = characterPort.getFixedCharacterObjectKey(imageId);
		if (avatarObjectKey == null) {
			log.warn("고정 캐릭터 이미지를 찾을 수 없습니다. imageId: {}", imageId);
			throw new ProfileImageNotFoundException(imageId);
		}

		user.changeAvatar(avatarObjectKey);
		saveUserPort.save(user);
	}

	private String getValidatedNewNickname(String userId, String newNickname, String oldNickname) {
		if (newNickname == null || newNickname.isEmpty() || newNickname.equals(oldNickname)) {
			return oldNickname;
		}

		long now = System.currentTimeMillis();
		NicknameHoldEntry hold = nicknameHoldMap.get(newNickname);
		if (hold == null || nicknamePolicy.isHoldExpired(hold.timestamp(), now) || !hold.userId().equals(userId)) {
			throw new UpdateProfileFailureException(
					UpdateProfileFailureReason.PROFILE_CONFLICT,
					"닉네임 점유 정보가 없거나 만료되었거나 본인이 아닙니다.");
		}
		if (userQueryPort.existsByNickname(newNickname)) {
			nicknameHoldMap.remove(newNickname);
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
			log.warn("Invalid character ID value: {}", characterId);
			throw new UpdateProfileFailureException(
					UpdateProfileFailureReason.INVALID_REQUEST,
					"유효하지 않은 캐릭터 ID입니다.");
		}
		String newAvatarObjectKey = characterPort.getFixedCharacterObjectKey(characterId);
		if (newAvatarObjectKey == null) {
			log.warn("Character not found for ID: {}", characterId);
			throw new UpdateProfileFailureException(
					UpdateProfileFailureReason.RESOURCE_NOT_FOUND,
					"존재하지 않는 캐릭터입니다.");
		}
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
