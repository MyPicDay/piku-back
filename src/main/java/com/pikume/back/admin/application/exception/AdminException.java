package com.pikume.back.admin.application.exception;

public class AdminException extends RuntimeException {

	private final AdminProblem problem;

	public AdminException(AdminProblem problem, String message) {
		super(message);
		this.problem = problem;
	}

	public AdminProblem problem() {
		return problem;
	}
}
