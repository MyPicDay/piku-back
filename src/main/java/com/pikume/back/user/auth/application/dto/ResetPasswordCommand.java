package com.pikume.back.user.auth.application.dto;

public record ResetPasswordCommand(String email, String newPassword) {
}
