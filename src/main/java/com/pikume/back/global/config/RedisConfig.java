package com.pikume.back.global.config;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);

		// Key는 String으로 직렬화
		template.setKeySerializer(new StringRedisSerializer());
		template.setHashKeySerializer(new StringRedisSerializer());

		// Value는 JSON으로 직렬화
		GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer()
				.configure(objectMapper -> objectMapper.registerModule(new JavaTimeModule()));
		template.setValueSerializer(valueSerializer);
		template.setHashValueSerializer(valueSerializer);

		template.afterPropertiesSet();
		return template;
	}
}
