package com.pikume.back.recommendation.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pikume.back.recommendation.domain.TopicAffinities;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

@Converter(autoApply = true)
@Slf4j
public class TopicAffinitiesConverter implements AttributeConverter<TopicAffinities, String> {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final TypeReference<LinkedHashMap<String, Double>> MAP_TYPE = new TypeReference<>() {
	};

	@Override
	public String convertToDatabaseColumn(TopicAffinities attribute) {
		Map<String, Double> values = attribute == null ? Map.of() : attribute.values();
		try {
			return OBJECT_MAPPER.writeValueAsString(values);
		} catch (JsonProcessingException exception) {
			log.warn("event=topic_affinities_serialize_failed reason={}",
					exception.getClass().getSimpleName());
			return "{}";
		}
	}

	@Override
	public TopicAffinities convertToEntityAttribute(String databaseValue) {
		if (databaseValue == null || databaseValue.isBlank() || databaseValue.equals("{}")) {
			return TopicAffinities.empty();
		}
		try {
			return TopicAffinities.from(OBJECT_MAPPER.readValue(databaseValue, MAP_TYPE));
		} catch (JsonProcessingException exception) {
			log.warn("event=topic_affinities_deserialize_failed outcome=empty reason={}",
					exception.getClass().getSimpleName());
			return TopicAffinities.empty();
		}
	}
}
