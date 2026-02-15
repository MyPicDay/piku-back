package store.piku.back.user._legacy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.global.error.ErrorCode;
import store.piku.back.global.exception.BusinessException;
import store.piku.back.global.util.ImagePathToUrlConverter;
import store.piku.back.user.application.port.out.LoadUserPort;
import store.piku.back.user.domain.User;

/**
 * 하위 호환용 UserReader
 * 외부 모듈(diary, auth, notification 등)이 사용하는 기존 UserReader를 유지합니다.
 * 내부적으로 LoadUserPort를 통해 조회를 위임합니다.
 *
 * @deprecated 다른 모듈이 헥사고날 아키텍처로 마이그레이션되면 제거 예정
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Deprecated
public class UserReader {

	private final LoadUserPort loadUserPort;
	private final ImagePathToUrlConverter imagePathToUrlConverter;

	/**
	 * 사용자 식별값을 입력받아 유저를 조회합니다.
	 */
	public User getUserById(String userId) {
		return loadUserPort.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
	}

	/**
	 * 이메일을 입력받아 유저를 조회합니다.
	 */
	public User getUserByEmail(String email) {
		return loadUserPort.findByEmail(email)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
	}

	/**
	 * 사용자 식별값을 입력받아 유저 정보를 반환합니다.
	 */
	public UserInfoDTO getUserInfoById(String userId, RequestMetaInfo requestMetaInfo) {
		User user = getUserById(userId);
		String avatarUrl = imagePathToUrlConverter.userAvatarImageUrl(user.getAvatar(), requestMetaInfo);
		return new UserInfoDTO(userId, user.getNickname(), avatarUrl);
	}

	/**
	 * 하위 호환용 UserInfoDTO (기존 user.dto.response.UserInfoDTO 대체)
	 */
	public record UserInfoDTO(String id, String nickname, String avatar) {
	}
}
