package com.pikume.back.admin.domain;

public enum AdminRole {
	SUPER_ADMIN,
	OPERATOR,
	VIEWER;

	public boolean isSuperAdmin() {
		return this == SUPER_ADMIN;
	}
}
