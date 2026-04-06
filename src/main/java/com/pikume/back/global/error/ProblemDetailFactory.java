package com.pikume.back.global.error;

import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Map;

@Component
public class ProblemDetailFactory {

	public ProblemDetail create(ApiProblemType problemType, String detail, String instance) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(problemType.status(), detail);
		problemDetail.setType(problemType.type());
		problemDetail.setTitle(problemType.title());
		problemDetail.setInstance(URI.create(instance));
		return problemDetail;
	}

	public ProblemDetail validation(String detail, String instance, Map<String, String> fieldErrors) {
		ProblemDetail problemDetail = create(ValidationProblemType.INVALID_REQUEST, detail, instance);
		problemDetail.setProperty("fieldErrors", fieldErrors);
		return problemDetail;
	}
}
