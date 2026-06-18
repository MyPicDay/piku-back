package com.pikume.back.admin.application.service;

public record AdminSessionCredentials(String sessionToken, String csrfToken) {
}
