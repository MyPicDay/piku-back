package store.piku.back.user.application.dto;

/**
 * 프로필 변경 요청 Command
 */
public record UpdateProfileCommand(
		String userId,
		String newNickname,
		Long characterId) {
}
