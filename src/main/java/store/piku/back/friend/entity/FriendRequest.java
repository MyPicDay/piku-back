package store.piku.back.friend.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import store.piku.back.friend.key.FriendRequestID;
import store.piku.back.global.entity.BaseEntity;

@IdClass(FriendRequestID.class)
@Table(name = "friend_request")
@SQLDelete(sql = "UPDATE friend_request SET deleted_at = NOW() WHERE from_user_id = ? AND to_user_id = ?")
@Where(clause = "deleted_at IS NULL")
@AllArgsConstructor
@Entity
public class FriendRequest extends BaseEntity {

    @Id
    @Column(name = "from_user_id")
    private String fromUserId;

    @Id
    @Column(name = "to_user_id")
    private String toUserId;


    public FriendRequest() {

    }
}
