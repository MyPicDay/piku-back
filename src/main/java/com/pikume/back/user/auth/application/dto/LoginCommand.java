package com.pikume.back.user.auth.application.dto;

public record LoginCommand(String email, String password, String deviceId) {
}
