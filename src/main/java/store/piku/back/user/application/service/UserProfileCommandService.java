package store.piku.back.user.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.StringUtils;
import store.piku.back.global.error.ErrorCode;
import store.piku.back.global.exception.BusinessException;
import store.piku.back.user.application.dto.UpdateProfileCommand;
import store.piku.back.user.application.dto.UpdateProfileResult;
import store.piku.back.user.application.port.in.CheckNicknameUseCase;
import store.piku.back.user.application.port.in.UpdateProfileUseCase;
import store.piku.back.user.application.port.out.LoadCharacterPort;
import store.piku.back.user.application.port.out.LoadUserPort;
import store.piku.back.user.application.port.out.SaveUserPort;
import store.piku.back.user.application.port.out.UserQueryPort;
import store.piku.back.user.domain.User;
import store.piku.back.user.domain.service.NicknamePolicy;

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
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
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
			return UpdateProfileResult.failure("변경할 닉네임이나 캐릭터 정보가 없습니다.", null);
		}

		User user = loadUserPort.findById(command.userId())
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		String oldNickname = user.getNickname();
		String oldAvatar = user.getAvatar();

		String targetNickname;
		try {
			targetNickname = getValidatedNewNickname(command.userId(), command.newNickname(), oldNickname);
		} catch (RuntimeException e) {
			return UpdateProfileResult.failure(e.getMessage(), oldNickname);
		}

		String targetAvatar;
		try {
			targetAvatar = getUpdatedAvatar(command.characterId(), oldAvatar);
		} catch (RuntimeException e) {
			return UpdateProfileResult.failure(e.getMessage(), oldNickname);
		}

		boolean nicknameChanged = !targetNickname.equals(oldNickname);
		boolean characterChanged = !targetAvatar.equals(oldAvatar);

		if (!nicknameChanged && !characterChanged) {
			return UpdateProfileResult.success("변경 사항이 없습니다.", oldNickname, targetAvatar);
		}

		try {
			User updatedUser = new User(user.getId(), user.getEmail(), user.getPassword(), targetNickname, targetAvatar);
			saveUserPort.save(updatedUser);

			return buildSuccessResult(nicknameChanged, characterChanged, targetNickname, targetAvatar);
		} finally {
			if (nicknameChanged) {
				nicknameHoldMap.remove(command.newNickname());
			}
		}
	}

	@Override
	@Transactional
	public boolean updateProfileImage(String userId, Long imageId) {
		User user = loadUserPort.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		if (!user.getId().equals(userId)) {
			throw new AccessDeniedException("본인만 프로필 이미지를 변경할 수 있습니다.");
		}

		String avatarUrl = characterPort.getFixedCharacterImageUrl(imageId);
		if (avatarUrl == null) {
			log.warn("고정 캐릭터 이미지를 찾을 수 없습니다. imageId: {}", imageId);
			return false;
		}

		user.changeAvatar(avatarUrl);
		saveUserPort.save(user);

		return true;
	}

	private String getValidatedNewNickname(String userId, String newNickname, String oldNickname) {
		if (newNickname == null || newNickname.isEmpty() || newNickname.equals(oldNickname)) {
			return oldNickname;
		}

		long now = System.currentTimeMillis();
		NicknameHoldEntry hold = nicknameHoldMap.get(newNickname);
		if (hold == null || nicknamePolicy.isHoldExpired(hold.timestamp(), now) || !hold.userId().equals(userId)) {
			throw new RuntimeException("닉네임 점유 정보가 없거나 만료되었거나 본인이 아닙니다.");
		}
		if (userQueryPort.existsByNickname(newNickname)) {
			nicknameHoldMap.remove(newNickname);
			throw new RuntimeException("이미 사용 중인 닉네임입니다.");
		}
		return newNickname;
	}

	private String getUpdatedAvatar(Long characterId, String oldAvatar) {
		if (characterId == null) {
			return oldAvatar;
		}
		try {
			String newCharacter = characterPort.getFixedCharacterImageUrl(characterId);
			if (newCharacter == null) {
				throw new IllegalArgumentException("존재하지 않는 캐릭터입니다.");
			}
			return newCharacter.equals(oldAvatar) ? oldAvatar : newCharacter;
		} catch (NumberFormatException e) {
			log.warn("Invalid character ID format: {}", characterId);
			throw new RuntimeException("유효하지 않은 캐릭터 ID 형식입니다.");
		} catch (IllegalArgumentException e) {
			log.warn("Character not found for ID: {}", characterId);
			throw new RuntimeException("존재하지 않는 캐릭터입니다.");
		}
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
