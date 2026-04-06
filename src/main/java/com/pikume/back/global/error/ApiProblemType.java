package com.pikume.back.global.error;

import org.springframework.http.HttpStatus;

import java.net.URI;

public interface ApiProblemType {

	URI type();

	HttpStatus status();

	String title();
}
