package com.pikume.back.global.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver;
import com.pikume.back.global.error.CommonProblemType;
import com.pikume.back.global.error.ErrorCode;
import com.pikume.back.global.error.ProblemDetailFactory;
import com.pikume.back.global.notification.DiscordWebhookService;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
public class ProblemDetailFallbackExceptionResolver extends AbstractHandlerExceptionResolver {

	private final ObjectMapper objectMapper;
	private final ProblemDetailFactory problemDetailFactory;
	private final Optional<DiscordWebhookService> discordWebhookService;

	public ProblemDetailFallbackExceptionResolver(
			ObjectMapper objectMapper,
			ProblemDetailFactory problemDetailFactory,
			Optional<DiscordWebhookService> discordWebhookService) {
		this.objectMapper = objectMapper;
		this.problemDetailFactory = problemDetailFactory;
		this.discordWebhookService = discordWebhookService;
		setOrder(Ordered.LOWEST_PRECEDENCE);
	}

	@Override
	protected ModelAndView doResolveException(@NotNull HttpServletRequest request, HttpServletResponse response, Object handler,
                                              @NotNull Exception ex) {
		if (response.isCommitted()) {
			return null;
		}

		log.error("Unhandled Exception occurred: {}", ex.getMessage(), ex);
		discordWebhookService.ifPresent(service -> service.sendExceptionNotification(ex, request));

		ProblemDetail problemDetail = problemDetailFactory.create(
				CommonProblemType.INTERNAL_SERVER_ERROR,
				ErrorCode.INTERNAL_SERVER_ERROR.getMessage(),
				request.getRequestURI());

		response.setStatus(CommonProblemType.INTERNAL_SERVER_ERROR.status().value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		try {
			objectMapper.writeValue(response.getWriter(), problemDetail);
		} catch (IOException writeFailure) {
			log.error("Failed to write fallback Problem Details response: {}", writeFailure.getMessage(), writeFailure);
			return null;
		}
		return new ModelAndView();
	}
}
