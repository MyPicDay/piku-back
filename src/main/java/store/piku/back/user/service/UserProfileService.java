package store.piku.back.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.piku.back.diary.dto.DiaryMonthCountDTO;
import store.piku.back.diary.enums.FriendStatus;
import store.piku.back.diary.service.DiaryService;
import store.piku.back.friend.service.FriendRequestService;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.global.util.ImagePathToUrlConverter;
import store.piku.back.user.dto.response.ProfilePreviewDTO;
import store.piku.back.user.dto.response.UserProfileResponseDTO;
import store.piku.back.user.entity.User;
import store.piku.back.user.service.reader.UserReader;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private final FriendRequestService friendService;
    private final DiaryService diaryService;
    private final ImagePathToUrlConverter imagePathToUrlConverter;
    private final UserReader userReader;

    /**
     * 사용자의 프로필 미리보기를 조회합니다.
     *
     * @param profileId 조회할 유저 식별값
     * @param userId    요청자 식별값
     */
    @Transactional(readOnly = true)
    public ProfilePreviewDTO getProfilePreviewByUserId(String profileId, String userId, RequestMetaInfo requestMetaInfo) {

        User profile = userReader.getUserById(profileId);
        String avatarUrl = imagePathToUrlConverter.userAvatarImageUrl(profile.getAvatar(), requestMetaInfo);
        int friendCount = friendService.countFriends(profileId);
        long diaryCount = diaryService.countDiariesByUserId(profileId);

        FriendStatus friendshipStatus = friendService.getFriendshipStatus(userId, profileId);

        log.info("사용자 ID {}에 대한 프로필 미리보기 조회 완료. 친구 수: {}, 일기 수: {}, 친구 상태: {}",profileId, friendCount, diaryCount, friendshipStatus);
        return new ProfilePreviewDTO(profileId, profile.getNickname(), avatarUrl, friendCount, diaryCount, friendshipStatus);
    }


    @Transactional(readOnly = true)
    public UserProfileResponseDTO getUserProfile(String profileId, String currentUserId, RequestMetaInfo requestMetaInfo) {
        ProfilePreviewDTO preview = getProfilePreviewByUserId(profileId, currentUserId, requestMetaInfo);
        boolean isOwner = profileId.equals(currentUserId);

        List<DiaryMonthCountDTO> monthlyDiaryCount = diaryService.getMonthlyDiaryCount(profileId);

        return new UserProfileResponseDTO(
                preview.getId(),
                preview.getNickname(),
                preview.getAvatar(),
                preview.getFriendCount(),
                preview.getDiaryCount(),
                preview.getFriendStatus(),
                isOwner,
                monthlyDiaryCount
        );
    }
}
