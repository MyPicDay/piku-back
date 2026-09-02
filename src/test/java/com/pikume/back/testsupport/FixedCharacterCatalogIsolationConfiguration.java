package com.pikume.back.testsupport;

import com.pikume.back.character.application.port.out.LoadFixedCharacterAssetsPort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;

@TestConfiguration(proxyBeanMethods = false)
public class FixedCharacterCatalogIsolationConfiguration {

	@Bean
	@Primary
	LoadFixedCharacterAssetsPort emptyFixedCharacterCatalog() {
		return List::of;
	}
}
