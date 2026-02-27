package com.pikume.back;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.pikume.back.diary.adapter.out.storage.StorageProperties;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableConfigurationProperties(StorageProperties.class)
public class PikuBackApplication {

	public static void main(String[] args) {
		SpringApplication.run(PikuBackApplication.class, args);
	}

}
