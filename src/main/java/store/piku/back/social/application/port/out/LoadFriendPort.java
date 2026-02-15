package store.piku.back.social.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import store.piku.back.social.domain.friend.Friend;

import java.util.List;

public interface LoadFriendPort {

	boolean existsFriendship(String userId1, String userId2);

	Page<Friend> findFriendsByUserId(String userId, Pageable pageable);

	int countByUserId(String userId);

	List<String> findFriendIds(String userId);
}
