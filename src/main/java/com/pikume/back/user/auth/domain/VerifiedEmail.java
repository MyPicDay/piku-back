package com.pikume.back.user.auth.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.pikume.back.user.auth.domain.vo.VerificationType;
import com.pikume.back.user.domain.vo.Email;

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
		this.email = new Email(email).value();
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

	public boolean isFor(String requestedEmail, VerificationType requestedType) {
		return email.equals(requestedEmail) && type == requestedType;
	}

	public boolean isUsed() {
		return Boolean.TRUE.equals(used);
	}
}
