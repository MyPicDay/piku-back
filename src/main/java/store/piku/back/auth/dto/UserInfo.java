package store.piku.back.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfo {
    private String id;
    private String email;
    private String nickname;
    private String avatar;
    private boolean isGuest;

    public UserInfo(String id, String email, String nickname, String avatar) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.avatar = avatar;
        this.isGuest = false;
    }
}
