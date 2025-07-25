package store.piku.back.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import store.piku.back.auth.enums.VerificationType;

import java.time.LocalDateTime;

@Entity
@Table(name = "verified_email")
@Getter
@NoArgsConstructor
public class VerifiedEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private LocalDateTime verifiedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationType type;

    @Column(nullable = false)
    private Boolean used;

    public VerifiedEmail(String email, VerificationType type) {
        this.email = email;
        this.type = type;
        this.verifiedAt = LocalDateTime.now();
        this.used = false;
    }

    public void markUsed() {
        if (used) {
            throw new IllegalStateException("이미 사용된 인증입니다.");
        }
        this.used = true;
    }

}

