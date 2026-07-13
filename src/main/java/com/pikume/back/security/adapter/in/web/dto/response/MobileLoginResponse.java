package com.pikume.back.security.adapter.in.web.dto.response;
public record MobileLoginResponse(String message, UserInfo user, MobileTokenBundle tokens) { }
