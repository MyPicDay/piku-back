package store.piku.back.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor
public class RefreshToken {
    @Id
    @Column(name = "refresh_key")
    private String key; // email + deviceId 조합

    private String refreshToken;

    public RefreshToken(String key, String refreshToken) {
        this.key = key;
        this.refreshToken = refreshToken;
    }

    public void updateToken(String newToken) {
        this.refreshToken = newToken;
    }
}
