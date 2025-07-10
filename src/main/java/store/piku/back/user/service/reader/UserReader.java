package store.piku.back.user.service.reader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.global.error.ErrorCode;
import store.piku.back.global.exception.BusinessException;
import store.piku.back.global.util.ImagePathToUrlConverter;
import store.piku.back.user.dto.response.UserInfoDTO;
import store.piku.back.user.entity.User;
import store.piku.back.user.exception.UserNotFoundException;
import store.piku.back.user.repository.UserRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserReader {

    private final UserRepository userRepository;
    private final ImagePathToUrlConverter imagePathToUrlConverter;

    /**
     * 사용자 식별값을 입력받아 유저를 조회합니다. 유저가 존재하지 않는다면 USER_NOT_FOUND 예외 발생.
     *
     * @param userId 조회할 유저 식별값
     * @return User 식별값 기준으로 유저를 조회하여 반환
     * @throws BusinessException 유저가 존재하지 않을 경우 USER_NOT_FOUND 에러 코드와 함께 발생
     */
    public User getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 사용자 식별값을 입력받아 유저 정보 DTO를 반환합니다. 유저가 존재하지 않는다면 USER_NOT_FOUND 예외 발생.
     * 유저 정보 DTO (id, nickname, avatar)
     *
     * @param userId 조회할 유저 식별값
     * @param requestMetaInfo 아바타 URL 생성을 위한 메타 정보
     * @return UserInfoDTO 식별값 기준으로 유저 정보를 조회하여 반환
     * @throws BusinessException 유저가 존재하지 않을 경우 USER_NOT_FOUND 에러 코드와 함께 발생
     */
    public UserInfoDTO getUserInfoById(String userId, RequestMetaInfo requestMetaInfo) {
        User user = getUserById(userId);
        String avatarUrl = imagePathToUrlConverter.userAvatarImageUrl(user.getAvatar(), requestMetaInfo);
        return new UserInfoDTO(userId, user.getNickname(), avatarUrl);
    }

}

