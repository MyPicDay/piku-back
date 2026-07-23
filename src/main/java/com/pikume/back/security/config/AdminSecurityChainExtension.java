package com.pikume.back.security.config;

import jakarta.servlet.Filter;

/**
 * Security가 Admin Web Adapter의 구체 타입을 알지 않고 Filter Chain에 연결하기 위한 확장 계약이다.
 */
public interface AdminSecurityChainExtension extends Filter {
}
