package store.piku.back.friend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class FriendsDto {

     // id
     private String userId;

    // 닉네임
     private String nickname;

    // 아바타
     private String avatar;

}
