package com.pikume.back.security.application.dto;

import com.pikume.back.security.dto.TokenDto;
public record LoginResult(TokenDto tokens, AuthenticatedUserInfo userInfo) {
}
