package com.pikume.back.security.principal;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class UserPrincipal implements UserDetails {

	private final String id;
	private final String nickname;
	private final String avatarPath;

	public UserPrincipal(String id, String nickname) {
		this(id, nickname, null);
	}

	public static UserPrincipal withAvatarPath(String id, String nickname, String avatarPath) {
		return new UserPrincipal(id, nickname, avatarPath);
	}

	private UserPrincipal(String id, String nickname, String avatarPath) {
		this.id = id;
		this.nickname = nickname;
		this.avatarPath = avatarPath;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_USER"));
	}

	@Override
	public String getPassword() {
		return null;
	}

	@Override
	public String getUsername() {
		return id;
	}
}
