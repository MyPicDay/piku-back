package store.piku.back.user.dto.response;


public class NicknameHold {
    private String userId;
    private long timestamp;

    public NicknameHold(String userId, long timestamp) {
        this.userId = userId;
        this.timestamp = timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
