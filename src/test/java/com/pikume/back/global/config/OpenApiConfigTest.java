package com.pikume.back.global.config;

import com.pikume.back.security.config.AdminSecurityProperties;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OpenApiConfig")
class OpenApiConfigTest {

	@Test
	@DisplayName("관리자 Swagger 계약은 환경별 세션 쿠키와 CSRF 헤더 이름을 사용한다")
	void publishesAdminSecuritySchemeNames() {
		OpenAPI openApi = config().openAPI();

		assertThat(openApi.getComponents().getSecuritySchemes().get("AdminSessionCookie").getName())
				.isEqualTo("pk-a91f");
		assertThat(openApi.getComponents().getSecuritySchemes().get("AdminCsrfHeader").getName())
				.isEqualTo("X-PK-C83F");
	}

	@Test
	@DisplayName("관리자 상태 변경 API는 세션과 CSRF를 함께 요구하고 초기화 API는 예외다")
	void appliesAdminSecurityRequirementsByOperation() {
		Operation login = new Operation();
		Operation csrf = new Operation();
		OpenAPI openApi = new OpenAPI().paths(new Paths()
				.addPathItem("/api/admin/auth/login", new PathItem().post(login))
				.addPathItem("/api/admin/auth/csrf", new PathItem().post(csrf)));

		config().adminSecurityCustomizer().customise(openApi);

		assertThat(login.getSecurity()).singleElement().satisfies(requirement ->
				assertThat(requirement.keySet()).containsExactlyInAnyOrder("AdminSessionCookie", "AdminCsrfHeader"));
		assertThat(csrf.getSecurity()).isEmpty();
	}

	@Test
	@DisplayName("Swagger 문서를 관리자 API와 사용자 API 그룹으로 분리한다")
	void separatesAdminAndUserApiGroups() {
		GroupedOpenApi admin = config().adminApi();
		GroupedOpenApi user = config().userApi();

		assertThat(admin.getGroup()).isEqualTo("admin");
		assertThat(admin.getDisplayName()).isEqualTo("관리자 API");
		assertThat(admin.getPathsToMatch()).containsExactly("/api/admin/**");

		assertThat(user.getGroup()).isEqualTo("user");
		assertThat(user.getDisplayName()).isEqualTo("사용자 API");
		assertThat(user.getPathsToMatch()).containsExactly("/api/**");
		assertThat(user.getPathsToExclude()).containsExactly("/api/admin/**");
	}

	private OpenApiConfig config() {
		return new OpenApiConfig(new AdminSecurityProperties(
				List.of("http://localhost:3000"), "pk-a91f", "pk-b74d", "X-PK-C83F", false, ""));
	}
}
