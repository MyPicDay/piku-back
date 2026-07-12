package com.pikume.back.user.auth.adapter.in.web.validation;

import com.pikume.back.user.domain.exception.InvalidEmailException;
import com.pikume.back.user.domain.vo.Email;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EmailFormatValidator implements ConstraintValidator<EmailFormat, String> {

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null || value.isBlank()) {
			return true;
		}

		try {
			new Email(value);
			return true;
		} catch (InvalidEmailException ignored) {
			return false;
		}
	}
}
