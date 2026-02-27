package com.pikume.back.global.dto;

public record RequestMetaInfo(
        String scheme,
        String domain,
        int port,
        String host,
        String fullUrl,
        String userAgent,
        String clientIp
) {}
