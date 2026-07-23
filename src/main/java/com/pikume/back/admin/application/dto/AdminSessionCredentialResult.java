package com.pikume.back.admin.application.dto;

public record AdminSessionCredentialResult(String sessionToken, String csrfToken) {
}
