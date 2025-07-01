package store.piku.back.friend.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import store.piku.back.friend.key.FriendID;

@IdClass(FriendID.class)
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "friend")
public class Friend {

    @Id
    @Column(name="user_id_1")
    private String userId1;

    @Id
    @Column(name = "user_id_2")
    private String userId2;
}
