package store.piku.back.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import store.piku.back.friend.dto.FriendsDTO;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.global.util.ImagePathToUrlConverter;
import store.piku.back.user.dto.response.NicknameHold;
import store.piku.back.user.dto.response.NicknameChangeResponseDTO;
import store.piku.back.user.entity.User;
import store.piku.back.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import store.piku.back.user.service.reader.UserReader;
import store.piku.back.character.service.CharacterService;
import store.piku.back.character.entity.Character;


import java.util.concurrent.ConcurrentHashMap;


@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final ImagePathToUrlConverter imagePathToUrlConverter;
    private final UserReader userReader;
    private final CharacterService characterService;


    private final ConcurrentHashMap<String, NicknameHold> nicknameHoldMap = new ConcurrentHashMap<>();
    private final long HOLD_DURATION_MS = 180000; // 300초 점유

    /**
     * 키워드로 유저를 검색해 친구 목록을 조회합니다.
     *
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @param requestMetaInfo 요청 메타 정보 (이미지 URL 변환용)
     * @return 친구 목록 페이지
     */
    public Page<FriendsDTO> searchByName(String keyword, Pageable pageable, RequestMetaInfo requestMetaInfo) {

        String formattedKeyword = "%" + keyword + "%";
        Page<User> users = userRepository.searchByName(formattedKeyword, pageable);

        return users.map(user -> {
            String avatarUrl = imagePathToUrlConverter.userAvatarImageUrl(user.getAvatar(), requestMetaInfo);
            return new FriendsDTO(user.getId(), user.getNickname(), avatarUrl);
        });

    }




    public boolean tryReserveNickname(String nickname, String userId) {

        long now = System.currentTimeMillis();

        User user = userReader.getUserById(userId);
        if (nickname.equals(user.getNickname())) return true;

        if (userRepository.existsByNickname(nickname)) return false;

        boolean reserved = nicknameHoldMap.compute(nickname, (key, hold) -> {
            if (hold == null || now - hold.getTimestamp() > HOLD_DURATION_MS) {
                return new NicknameHold(userId, now);
            }
            return hold;
        }).getUserId().equals(userId);

        return reserved;
    }



    @Transactional
    public NicknameChangeResponseDTO reserveAndChangeNickname(String userId, String newNickname, String characterId) {
        if ((newNickname == null || newNickname.isEmpty()) && (characterId == null || characterId.isEmpty())) {
            return new NicknameChangeResponseDTO(false, "변경할 닉네임이나 캐릭터 정보가 없습니다.", null, null);
        }

        User user = userReader.getUserById(userId);
        String oldNickname = user.getNickname();
        String oldAvatar = user.getAvatar();

        String targetNickname;
        try {
            targetNickname = getValidatedNewNickname(userId, newNickname, oldNickname);
        } catch (RuntimeException e) {
            return new NicknameChangeResponseDTO(false, e.getMessage(), oldNickname, null);
        }

        String targetAvatar;
        try {
            targetAvatar = getUpdatedAvatar(characterId, oldAvatar);
        } catch (RuntimeException e) {
            return new NicknameChangeResponseDTO(false, e.getMessage(), oldNickname, null);
        }

        boolean nicknameChanged = !targetNickname.equals(oldNickname);
        boolean characterChanged = !targetAvatar.equals(oldAvatar);

        if (!nicknameChanged && !characterChanged) {
            return new NicknameChangeResponseDTO(true, "변경 사항이 없습니다.", oldNickname, characterId);
        }

        try {
            User updatedUser = new User(user.getId(), user.getEmail(), user.getPassword(), targetNickname, targetAvatar);
            userRepository.save(updatedUser);

            return buildSuccessResponse(nicknameChanged, characterChanged, targetNickname, characterId);
        } finally {
            if (nicknameChanged) {
                nicknameHoldMap.remove(newNickname);
            }
        }
    }

    private String getValidatedNewNickname(String userId, String newNickname, String oldNickname) {
        if (newNickname == null || newNickname.isEmpty() || newNickname.equals(oldNickname)) {
            return oldNickname;
        }

        long now = System.currentTimeMillis();
        NicknameHold hold = nicknameHoldMap.get(newNickname);
        if (hold == null || now - hold.getTimestamp() > HOLD_DURATION_MS || !hold.getUserId().equals(userId)) {
            throw new RuntimeException("닉네임 점유 정보가 없거나 만료되었거나 본인이 아닙니다.");
        }
        if (userRepository.existsByNickname(newNickname)) {
            nicknameHoldMap.remove(newNickname);
            throw new RuntimeException("이미 사용 중인 닉네임입니다.");
        }
        return newNickname;
    }

    private String getUpdatedAvatar(String characterId, String oldAvatar) {
        if (characterId == null || characterId.isEmpty()) {
            return oldAvatar;
        }
        try {
            Character character = characterService.getCharacterById(Long.parseLong(characterId));
            return character.getImageUrl().equals(oldAvatar) ? oldAvatar : character.getImageUrl();
        } catch (NumberFormatException e) {
            log.warn("Invalid character ID format: {}", characterId);
            throw new RuntimeException("유효하지 않은 캐릭터 ID 형식입니다.");
        } catch (IllegalArgumentException e) {
            log.warn("Character not found for ID: {}", characterId);
            throw new RuntimeException("존재하지 않는 캐릭터입니다.");
        }
    }

    private NicknameChangeResponseDTO buildSuccessResponse(boolean nicknameChanged, boolean characterChanged, String nickname, String characterId) {
        String message;
        if (nicknameChanged && characterChanged) {
            message = "닉네임과 캐릭터가 성공적으로 변경되었습니다.";
        } else if (nicknameChanged) {
            message = "닉네임이 성공적으로 변경되었습니다.";
        } else { // characterChanged
            message = "캐릭터가 성공적으로 변경되었습니다.";
        }
        return new NicknameChangeResponseDTO(true, message, nickname, characterChanged ? characterId : null);
    }
}

