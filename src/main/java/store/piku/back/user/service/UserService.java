package store.piku.back.user.service;

import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import store.piku.back.friend.dto.FriendsDTO;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.global.util.ImagePathToUrlConverter;
import store.piku.back.user.dto.response.NicknameHold;
import store.piku.back.user.dto.response.NicknameChangeResponseDTO;
import store.piku.back.user.entity.User;
import store.piku.back.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import store.piku.back.user.service.reader.UserReader;

import java.util.concurrent.ConcurrentHashMap;


@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final ImagePathToUrlConverter imagePathToUrlConverter;
    private final UserReader userReader;


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




    public NicknameChangeResponseDTO reserveAndChangeNickname(String userId, String newNickname) {
        long now = System.currentTimeMillis();

        NicknameHold hold = nicknameHoldMap.get(newNickname);
        if (hold == null || now - hold.getTimestamp() > HOLD_DURATION_MS || !hold.getUserId().equals(userId)) {
            return new NicknameChangeResponseDTO(false, "점유 정보가 없거나 만료되었거나 본인이 아닙니다.", newNickname);
        }

        if (userRepository.existsByNickname(newNickname)) {
            nicknameHoldMap.remove(newNickname);
            return new NicknameChangeResponseDTO(false, "이미 사용 중인 닉네임입니다.", newNickname);
        }



            try {
                userRepository.updateNickname(userId, newNickname);
                return new NicknameChangeResponseDTO(true, "닉네임 변경 완료", newNickname);
            } catch (DataIntegrityViolationException | ConstraintViolationException e) {
                // DB에 동일 닉네임이 존재해서 UNIQUE 제약 위반
                return new NicknameChangeResponseDTO(false, "이미 사용 중인 닉네임입니다. (DB 충돌)", newNickname);
            }
        }
    }



