package com.pikume.back.admin.application.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AdminIdentifierMasker {

	private static final Pattern EMAIL_IN_TEXT = Pattern.compile(
			"[^\\s@,;:()<>]+@[^\\s@,;:()<>]+\\.[^\\s@,;:()<>]+");

	private AdminIdentifierMasker() {
	}

	static String maskEmail(String email) {
		if (email == null) {
			return null;
		}
		int atIndex = email.indexOf('@');
		if (atIndex <= 0) {
			return "***";
		}
		String localPart = email.substring(0, atIndex);
		String visiblePrefix = localPart.substring(0, Math.min(2, localPart.length()));
		return visiblePrefix + "***" + email.substring(atIndex);
	}

	static String maskLoginId(String loginId) {
		if (loginId == null) {
			return null;
		}
		if (loginId.length() <= 2) {
			return loginId.substring(0, 1) + "***";
		}
		if (loginId.length() <= 4) {
			return loginId.substring(0, 2) + "***";
		}
		return loginId.substring(0, 2) + "***" + loginId.substring(loginId.length() - 2);
	}

	static String maskEmailsInText(String text) {
		if (text == null) {
			return null;
		}
		Matcher matcher = EMAIL_IN_TEXT.matcher(text);
		StringBuffer masked = new StringBuffer();
		while (matcher.find()) {
			matcher.appendReplacement(masked, Matcher.quoteReplacement(maskEmail(matcher.group())));
		}
		matcher.appendTail(masked);
		return masked.toString();
	}

	static String removeEmailsFromText(String text) {
		if (text == null) {
			return null;
		}
		return EMAIL_IN_TEXT.matcher(text).replaceAll("[email removed]");
	}
}
