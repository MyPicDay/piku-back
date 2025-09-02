package store.piku.back.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import store.piku.back.auth.enums.Role;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor
public class RefreshToken {
    @Id
    @Column(name = "refresh_key")
    private String key; // email + deviceId 조합

    private String refreshToken;

    private String userId;

    private Role role;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;

    public void inactive() {
        this.deletedAt = LocalDateTime.now();
    }

    public RefreshToken(String key, String refreshToken, String userId, Role role) {
        this.key = key;
        this.refreshToken = refreshToken;
        this.userId = userId;
        this.role = role;
    }

    public void updateToken(String newToken) {
        this.refreshToken = newToken;
    }
}
