package com.pikume.back.creative.application.policy;

import org.springframework.stereotype.Component;

@Component
public class CharacterReferencePolicy {

	public boolean isAbsoluteUrl(String reference) {
		return reference != null
				&& (reference.startsWith("http://") || reference.startsWith("https://"));
	}
}
