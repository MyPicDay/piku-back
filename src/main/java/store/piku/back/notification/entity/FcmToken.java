package store.piku.back.notification.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Table(name = "FCM")
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FcmToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId; // 유저 ID

    @Column(unique = true)
    private String token;

    public FcmToken(String userId, String token) {
        this.userId = userId;
        this.token = token;
    }
    public void updateToken(String newToken) {
        this.token = newToken;
    }

}
