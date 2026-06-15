package com.pikume.back.architecture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Architecture boundaries")
class ArchitectureBoundaryTest {

	@Test
	@DisplayName("security outbound adapters는 user persistence adapters 또는 user domain entities에 대한 의존성을 갖지 않는다.")
	void securityOutboundAdaptersDoNotDependOnUserPersistenceOrDomain() throws IOException {
		Path securityOutboundAdapters = Path.of("src/main/java/com/pikume/back/security/adapter/out");
		List<String> violations;

		try (var paths = Files.walk(securityOutboundAdapters)) {
			violations = paths
					.filter(path -> path.toString().endsWith(".java"))
					.filter(this::importsUserPersistenceOrDomain)
					.map(Path::toString)
					.toList();
		}

		assertThat(violations).isEmpty();
	}

	private boolean importsUserPersistenceOrDomain(Path path) {
		try {
			String source = Files.readString(path);
			return source.contains("com.pikume.back.user.adapter.out.persistence")
					|| source.contains("com.pikume.back.user.domain.User");
		} catch (IOException e) {
			throw new IllegalStateException("Failed to read " + path, e);
		}
	}
}
