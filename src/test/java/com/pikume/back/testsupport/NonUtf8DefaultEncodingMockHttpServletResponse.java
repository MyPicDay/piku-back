package com.pikume.back.testsupport;

import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;

public class NonUtf8DefaultEncodingMockHttpServletResponse extends MockHttpServletResponse {

	private String contentType;
	private String characterEncoding = StandardCharsets.ISO_8859_1.name();

	@Override
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	@Override
	public String getContentType() {
		return contentType;
	}

	@Override
	public void setCharacterEncoding(String characterEncoding) {
		this.characterEncoding = characterEncoding;
		super.setCharacterEncoding(characterEncoding);
	}

	@Override
	public String getCharacterEncoding() {
		return characterEncoding;
	}
}
