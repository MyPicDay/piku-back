package store.piku.back.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import store.piku.back.auth.enums.VerificationType;

import java.time.LocalDateTime;

@Entity
@Table(name = "verification")
@Getter
@NoArgsConstructor
public class Verification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationType type;

    public Verification(String email, String code, VerificationType type) {
        this.email = email;
        this.code = code;
        this.type = type;
        this.expiresAt = LocalDateTime.now().plusMinutes(5);
    }

    // 인증 코드 정보 업데이트 (재전송 시 사용)
    public void updateCode(String newCode) {
        this.code = newCode;
        this.expiresAt = LocalDateTime.now().plusMinutes(5);
    }
}
