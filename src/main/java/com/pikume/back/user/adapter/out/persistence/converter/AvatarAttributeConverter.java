package com.pikume.back.user.adapter.out.persistence.converter;

import com.pikume.back.user.domain.vo.Avatar;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AvatarAttributeConverter implements AttributeConverter<Avatar, String> {

	@Override
	public String convertToDatabaseColumn(Avatar attribute) {
		return attribute == null ? null : attribute.path();
	}

	@Override
	public Avatar convertToEntityAttribute(String dbData) {
		return new Avatar(dbData);
	}
}
