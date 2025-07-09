package store.piku.back.friend.entity;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import store.piku.back.friend.key.FriendID;

@IdClass(FriendID.class)
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "friend")
@Getter
public class Friend {

    @Id
    @Column(name="user_id_1")
    private String userId1;

    @Id
    @Column(name = "user_id_2")
    private String userId2;

    @CreatedDate
    @Column(updatable = false)
    private String createdAt;

    public Friend(String userId1, String userId2) {
        this.userId1 = userId1;
        this.userId2 = userId2;
    }
}
