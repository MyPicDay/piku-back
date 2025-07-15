package store.piku.back.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import store.piku.back.global.entity.BaseEntity;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    @Column(unique = true, nullable = false)
    private String nickname;

    private String avatar;

    public User(String email, String password, String nickname, String avatar) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.avatar = avatar;
    }

    public User(String email, String password, String nickname) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
    }

    public User(String id, String email, String password, String newNickname, String avatar) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.nickname = newNickname;
        this.avatar = avatar;
    }

    public void changeAvatar(String avatar) {
        this.avatar = avatar;

    }
}
