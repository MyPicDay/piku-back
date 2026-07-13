package com.pikume.back.user.adapter.out.persistence.converter;

import com.pikume.back.user.domain.vo.Email;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EmailAttributeConverter implements AttributeConverter<Email, String> {

	@Override
	public String convertToDatabaseColumn(Email attribute) {
		return attribute == null ? null : attribute.value();
	}

	@Override
	public Email convertToEntityAttribute(String dbData) {
		return dbData == null ? null : new Email(dbData);
	}
}
