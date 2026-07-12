package com.pikume.back.user.auth.application.dto;

import com.pikume.back.user.auth.domain.vo.VerificationType;

public record VerifyEmailCommand(String email, String code, VerificationType type) {
}
