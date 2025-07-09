package store.piku.back.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import store.piku.back.friend.dto.FriendsDTO;
import store.piku.back.user.entity.User;
import store.piku.back.user.exception.UserExceptionMessage;
import store.piku.back.user.exception.UserNotFoundException;
import store.piku.back.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;

    public User getUserById(String userId) throws UserNotFoundException {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error(UserExceptionMessage.USER_NOT_FOUND.getMessage().formatted(userId));
                    return new UserNotFoundException(UserExceptionMessage.USER_NOT_FOUND.getMessage().formatted(userId));
                });
    }


    public Page<FriendsDTO> searchByName(String keyword, Pageable pageable) {
        return userRepository.searchByName(keyword, pageable);
    }
}
