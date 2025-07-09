package store.piku.back.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import store.piku.back.diary.enums.FriendStatus;

@Data
@AllArgsConstructor
public class ProfilePreviewDTO {
    private String id;
    private String nickname;
    private String avatar;
    private int friendCount;
    private long diaryCount;
    private FriendStatus friendStatus;
}
