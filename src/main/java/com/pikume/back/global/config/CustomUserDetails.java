package com.pikume.back.global.config;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails implements UserDetails {
    private final String id;
    private final String email;
    private final String nickname;
    private final String avatarPath;

    public CustomUserDetails(String id, String email, String nickname) {
        this(id, email, nickname, null);
    }

    public static CustomUserDetails withAvatarPath(String id, String email, String nickname, String avatarPath) {
        return new CustomUserDetails(id, email, nickname, avatarPath);
    }

    private CustomUserDetails(String id, String email, String nickname, String avatarPath) {
        this.id = id;
        this.email = email;
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
        return email;
    }
}
