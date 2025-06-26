package store.piku.back.friend.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import store.piku.back.friend.key.FriendID;
import store.piku.back.global.entity.BaseEntity;

@IdClass(FriendID.class)
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "friend")
public class Friend extends BaseEntity {

    @Id
    @Column(name="user_id_1")
    private String userId1;

    @Id
    @Column(name = "user_id_2")
    private String userId2;
}
