package com.pikume.back.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;

import java.util.List;

@Profile("dev")
@Configuration
public class OpenApiConfig {
	private static final String JWT_SCHEME = "JWT";
	private static final String ADMIN_SESSION_SCHEME = "AdminSessionCookie";
	private static final String ADMIN_CSRF_SCHEME = "AdminCsrfHeader";

	private final OpenApiSecuritySchemeNames securitySchemeNames;

	public OpenApiConfig(OpenApiSecuritySchemeNames securitySchemeNames) {
		this.securitySchemeNames = securitySchemeNames;
	}

	@Bean
	public GroupedOpenApi adminApi() {
		return GroupedOpenApi.builder()
				.group("admin")
				.displayName("관리자 API")
				.pathsToMatch("/api/admin/**")
				.build();
	}

	@Bean
	public GroupedOpenApi userApi() {
		return GroupedOpenApi.builder()
				.group("user")
				.displayName("사용자 API")
				.pathsToMatch("/api/**")
				.pathsToExclude("/api/admin/**")
				.build();
	}

	@Bean
	public OpenAPI openAPI() {
		Info info = new Info()
				.title("Piku API Documentation")
				.version("v1.0.0")
				.description("Piku project API 명세서입니다.");

		// Security Schema Name
		// API 요청 헤더에 인증 정보 포함
		SecurityRequirement securityRequirement = new SecurityRequirement().addList(JWT_SCHEME);
		// SecuritySchemes 등록
		Components components = new Components()
				.addSecuritySchemes(JWT_SCHEME, new SecurityScheme()
						.name(JWT_SCHEME)
						.type(SecurityScheme.Type.HTTP) // HTTP 방식
						.scheme("bearer")
						.bearerFormat("JWT")) // 토큰 형식 지정
				.addSecuritySchemes(ADMIN_SESSION_SCHEME, new SecurityScheme()
						.type(SecurityScheme.Type.APIKEY)
						.in(SecurityScheme.In.COOKIE)
						.name(securitySchemeNames.adminSessionCookie()))
				.addSecuritySchemes(ADMIN_CSRF_SCHEME, new SecurityScheme()
						.type(SecurityScheme.Type.APIKEY)
						.in(SecurityScheme.In.HEADER)
						.name(securitySchemeNames.adminCsrfHeader()));

		return new OpenAPI()
				.info(info)
				.addSecurityItem(securityRequirement)
				.components(components);
	}

	@Bean
	public OpenApiCustomizer adminSecurityCustomizer() {
		return openApi -> {
			if (openApi.getPaths() == null) {
				return;
			}
			openApi.getPaths().forEach((path, item) -> {
				if (!path.startsWith("/api/admin/")) {
					return;
				}
				item.readOperationsMap().forEach((method, operation) -> {
					if (path.equals("/api/admin/auth/csrf")) {
						operation.setSecurity(List.of());
						return;
					}
					SecurityRequirement requirement = new SecurityRequirement().addList(ADMIN_SESSION_SCHEME);
					if (method != io.swagger.v3.oas.models.PathItem.HttpMethod.GET
							&& method != io.swagger.v3.oas.models.PathItem.HttpMethod.HEAD
							&& method != io.swagger.v3.oas.models.PathItem.HttpMethod.OPTIONS) {
						requirement.addList(ADMIN_CSRF_SCHEME);
					}
					operation.setSecurity(List.of(requirement));
				});
			});
		};
	}
}
