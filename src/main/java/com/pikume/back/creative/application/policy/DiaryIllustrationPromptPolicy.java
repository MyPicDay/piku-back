package com.pikume.back.creative.application.policy;

import org.springframework.stereotype.Component;

@Component
public class DiaryIllustrationPromptPolicy {

	public String createPrompt(String content) {
		StringBuilder sb = new StringBuilder();

		sb.append("Using the provided character reference image(s), ");
		sb.append("generate ONE image of the SAME character performing this diary action: ");
		sb.append(content).append(". ");

		sb.append("Follow ALL requirements: ");
		sb.append(
				"Identity & Style Consistency: Match the character's facial features, hairstyle, skin tone, body proportions, clothing palette, and art style from the reference image(s). ");
		sb.append(
				"Scene: Build a coherent, artistic scene that supports the action with appropriate environment, props, and a natural pose. ");
		sb.append(
				"Composition & Camera: Use clear framing or angle that showcases the action, cinematic lighting that matches the mood, and avoid awkward crops. ");
		sb.append("Quality: High detail with clean rendering and correct anatomy, hands, and fingers. ");

		sb.append("Text Rendering Rule: ");
		if (hasDesignTextIntent(content)) {
			sb.append(
					"If the scene intentionally includes a design element that contains text such as speech bubbles, signs, posters, UI panels, or clothing prints, ");
			sb.append("you may render short, readable text only inside those elements. ");
		} else {
			sb.append("Render no text anywhere: no words, letters, numbers, logos, watermarks, or captions. ");
			sb.append("If an object would normally have text, leave it blank or use non-legible abstract marks. ");
		}

		sb.append("Output: A single finished image only. ");
		sb.append(
				"Constraints: Respect the reference style, lighting, and perspective, and keep the background coherent but not distracting.");

		return sb.toString();
	}

	boolean hasDesignTextIntent(String content) {
		String lower = content.toLowerCase();
		return lower.contains("말풍선") || lower.contains("간판") || lower.contains("표지판")
				|| lower.contains("사인") || lower.contains("포스터") || lower.contains("배너")
				|| lower.contains("현수막") || lower.contains("간판문구")
				|| lower.contains("ui") || lower.contains("패널")
				|| lower.contains("티셔츠 프린트") || lower.contains("텍스트")
				|| lower.contains("speech bubble") || lower.contains("sign")
				|| lower.contains("poster") || lower.contains("banner") || lower.contains("caption");
	}
}
