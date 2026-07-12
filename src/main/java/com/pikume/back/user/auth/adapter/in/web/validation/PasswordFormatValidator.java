package com.pikume.back.user.auth.adapter.in.web.validation;

import com.pikume.back.user.domain.exception.InvalidPasswordException;
import com.pikume.back.user.domain.service.PasswordPolicy;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordFormatValidator implements ConstraintValidator<PasswordFormat, String> {

	private static final PasswordPolicy PASSWORD_POLICY = new PasswordPolicy();

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null || value.isBlank()) {
			return true;
		}

		try {
			PASSWORD_POLICY.validate(value);
			return true;
		} catch (InvalidPasswordException ignored) {
			return false;
		}
	}
}
