package com.pikume.back.security.application.dto;

import com.pikume.back.security.dto.TokenDto;
import com.pikume.back.security.dto.UserInfo;

public record LoginResult(TokenDto tokens, UserInfo userInfo) {
}
