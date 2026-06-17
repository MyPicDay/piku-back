package com.pikume.back.admin.adapter.out.security;

import com.pikume.back.admin.application.port.out.GenerateTemporaryPasswordPort;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class SecureTemporaryPasswordGenerator implements GenerateTemporaryPasswordPort {

	private static final char[] UPPERCASE = "ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
	private static final char[] LOWERCASE = "abcdefghijkmnopqrstuvwxyz".toCharArray();
	private static final char[] DIGITS = "23456789".toCharArray();
	private static final char[] SPECIALS = "!@#$%^&*".toCharArray();
	private static final char[] ALL = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%^&*".toCharArray();

	private final SecureRandom secureRandom = new SecureRandom();

	@Override
	public String generate() {
		List<Character> characters = new ArrayList<>();
		characters.add(random(UPPERCASE));
		characters.add(random(LOWERCASE));
		characters.add(random(DIGITS));
		characters.add(random(SPECIALS));
		while (characters.size() < 16) {
			characters.add(random(ALL));
		}
		Collections.shuffle(characters, secureRandom);

		StringBuilder password = new StringBuilder(16);
		for (Character character : characters) {
			password.append(character);
		}
		return password.toString();
	}

	private char random(char[] source) {
		return source[secureRandom.nextInt(source.length)];
	}
}
