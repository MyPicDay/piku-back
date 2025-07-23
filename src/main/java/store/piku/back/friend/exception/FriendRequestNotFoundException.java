package store.piku.back.friend.exception;

public class FriendRequestNotFoundException extends FriendException {
    public FriendRequestNotFoundException(String message) {
        super(message);
    }
}
/*
요청이 존재할 것으로 기대했는데 없을 때" 사용합니다.

즉, 주로 요청 수락/거절/취소 등의 후속 처리에서 사용
 */
