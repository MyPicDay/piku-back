package com.pikume.back.security.adapter.out.persistence;

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
public class RefreshSessionEntity {
	@Id
	@Column(name = "refresh_key")
	private String key;
	private String refreshToken;
	private String userId;

	public RefreshSessionEntity(String key, String refreshToken, String userId) {
		this.key = key;
		this.refreshToken = refreshToken;
		this.userId = userId;
	}
}
