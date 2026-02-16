package store.piku.back.user.auth.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import store.piku.back.user.auth.domain.vo.VerificationType;

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

	public void updateCode(String newCode) {
		this.code = newCode;
		this.expiresAt = LocalDateTime.now().plusMinutes(5);
	}
}
