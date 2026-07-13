package com.pikume.back.user.adapter.out.persistence.converter;

import com.pikume.back.user.domain.vo.Nickname;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class NicknameAttributeConverter implements AttributeConverter<Nickname, String> {

	@Override
	public String convertToDatabaseColumn(Nickname attribute) {
		return attribute == null ? null : attribute.value();
	}

	@Override
	public Nickname convertToEntityAttribute(String dbData) {
		return dbData == null ? null : new Nickname(dbData);
	}
}
