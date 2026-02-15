package store.piku.back.user.application.port.out;

/**
 * 친구 정보 조회 Outbound Port (타 도메인 의존성 추상화)
 */
public interface UserFriendPort {

	/**
	 * 사용자의 친구 수를 조회합니다.
	 */
	int countFriends(String userId);

	/**
	 * 두 사용자 간 친구 관계 상태를 조회합니다.
	 *
	 * @param userId       요청자 ID
	 * @param targetUserId 대상 사용자 ID
	 * @return 친구 상태 문자열
	 */
	String getFriendshipStatus(String userId, String targetUserId);
}
