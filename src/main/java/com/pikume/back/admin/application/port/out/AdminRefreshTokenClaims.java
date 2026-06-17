package com.pikume.back.admin.application.port.out;

public record AdminRefreshTokenClaims(String adminId, String sessionId) {
}
